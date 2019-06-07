package com.tughi.aggregator.activities.feedsettings

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.tagspicker.TagsPickerActivity
import com.tughi.aggregator.activities.updatemode.UpdateModeActivity
import com.tughi.aggregator.activities.updatemode.startUpdateModeActivity
import com.tughi.aggregator.activities.updatemode.toString
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.FeedTags
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FaviconUpdateScheduler
import com.tughi.aggregator.utilities.backupFeeds
import com.tughi.aggregator.widgets.DropDownButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FeedSettingsFragment : Fragment() {

    companion object {
        const val ARG_FEED_ID = "feed_id"

        const val REQUEST_TAGS = 1
        const val REQUEST_UPDATE_MODE = 2
    }

    private lateinit var urlEditText: EditText
    private lateinit var titleEditText: EditText
    private lateinit var updateModeView: DropDownButton
    private lateinit var tagsView: DropDownButton

    private lateinit var viewModel: FeedSettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.feed_settings_fragment, container, false)

        urlEditText = fragmentView.findViewById(R.id.url)
        titleEditText = fragmentView.findViewById(R.id.title)
        updateModeView = fragmentView.findViewById(R.id.update_mode)
        tagsView = fragmentView.findViewById(R.id.tags)

        updateModeView.setOnClickListener {
            val feed = viewModel.feed.value ?: return@setOnClickListener
            startUpdateModeActivity(REQUEST_UPDATE_MODE, viewModel.newUpdateMode ?: feed.updateMode)
        }

        tagsView.setOnClickListener {
            val feedTags = viewModel.feedTags.value ?: return@setOnClickListener
            TagsPickerActivity.startForResult(this, REQUEST_TAGS, selectedTags = LongArray(feedTags.size) { feedTags[it].id }, title = getString(R.string.feed_tags))
        }

        val feedId = arguments!!.getLong(ARG_FEED_ID)
        viewModel = ViewModelProviders.of(this, FeedSettingsViewModel.Factory(feedId)).get(FeedSettingsViewModel::class.java)

        viewModel.feed.observe(this, Observer { feed ->
            if (feed != null) {
                urlEditText.setText(feed.url)
                titleEditText.setText(feed.customTitle ?: feed.title)
                updateModeView.setText(feed.updateMode.toString(updateModeView.context))
            }
        })

        viewModel.feedTags.observe(this, Observer { feedTags ->
            if (feedTags != null && feedTags.isNotEmpty()) {
                tagsView.setText(feedTags.joinToString())
            } else {
                tagsView.setText(R.string.feed_settings__tags__none)
            }
        })

        fragmentView.findViewById<View>(R.id.unsubscribe).setOnClickListener {
            viewModel.feed.value?.let {
                UnsubscribeDialogFragment.show(fragmentManager!!, it.id, it.customTitle ?: it.title, true)
            }
        }

        return fragmentView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_TAGS -> {
                    val selectedTags = data?.getLongArrayExtra(TagsPickerActivity.EXTRA_SELECTED_TAGS) ?: return
                    viewModel.newSelectedTagIds.value = selectedTags
                }
                REQUEST_UPDATE_MODE -> {
                    val serializedUpdateMode = data?.getStringExtra(UpdateModeActivity.EXTRA_UPDATE_MODE) ?: return
                    viewModel.newUpdateMode = UpdateMode.deserialize(serializedUpdateMode).also { updateMode ->
                        updateModeView.setText(updateMode.toString(updateModeView.context))
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater?.inflate(R.menu.feed_settings_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.save -> onSave()
        else -> super.onOptionsItemSelected(item)
    }

    private fun onSave(): Boolean {
        val url = urlEditText.text.toString().trim()
        val title = titleEditText.text.toString().trim()
        val updateMode = viewModel.newUpdateMode

        val oldSelectedTagIds = viewModel.oldSelectedTagIds
        val newSelectedTagIds = viewModel.newSelectedTagIds.value ?: LongArray(0)

        viewModel.feed.value?.let { feed ->
            GlobalScope.launch {
                Database.transaction {
                    Feeds.update(
                            Feeds.UpdateRowCriteria(feed.id),
                            Feeds.URL to url,
                            Feeds.CUSTOM_TITLE to if (title.isEmpty() || title == feed.title) null else title,
                            Feeds.UPDATE_MODE to (updateMode ?: feed.updateMode).serialize()
                    )

                    for (tagId in newSelectedTagIds) {
                        if (!oldSelectedTagIds.contains(tagId)) {
                            FeedTags.insert(
                                    FeedTags.FEED_ID to feed.id,
                                    FeedTags.TAG_ID to tagId,
                                    FeedTags.TAG_TIME to System.currentTimeMillis()
                            )
                        }
                    }

                    for (tagId in oldSelectedTagIds) {
                        if (!newSelectedTagIds.contains(tagId)) {
                            FeedTags.delete(FeedTags.DeleteFeedTagCriteria(feed.id, tagId))
                        }
                    }
                }

                if (updateMode != null && updateMode != feed.updateMode) {
                    AutoUpdateScheduler.scheduleFeed(feed.id)
                }

                GlobalScope.launch {
                    backupFeeds()
                }

                launch(Dispatchers.Main) {
                    FaviconUpdateScheduler.schedule(feed.id)

                    activity?.finish()
                }
            }
        }

        return true
    }

    class Feed(
            val id: Long,
            val url: String,
            val title: String,
            val customTitle: String?,
            val updateMode: UpdateMode,
            val tags: String?
    ) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
                Feeds.ID,
                Feeds.URL,
                Feeds.TITLE,
                Feeds.CUSTOM_TITLE,
                Feeds.UPDATE_MODE,
                Feeds.TAG_NAMES
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                    id = cursor.getLong(0),
                    url = cursor.getString(1),
                    title = cursor.getString(2),
                    customTitle = cursor.getString(3),
                    updateMode = UpdateMode.deserialize(cursor.getString(4)),
                    tags = cursor.getString(5)
            )
        }
    }

    class FeedTag(
            val id: Long,
            val name: String
    ) {
        override fun toString(): String = name

        object QueryHelper : FeedTags.QueryHelper<FeedTag>(
                FeedTags.TAG_ID,
                FeedTags.TAG_NAME
        ) {
            override fun createRow(cursor: Cursor) = FeedTag(
                    id = cursor.getLong(0),
                    name = cursor.getString(1)
            )
        }
    }

    class Tag(
            val id: Long,
            val name: String
    ) {
        override fun toString(): String = name

        object QueryHelper : Tags.QueryHelper<Tag>(
                Tags.ID,
                Tags.NAME
        ) {
            override fun createRow(cursor: Cursor) = Tag(
                    id = cursor.getLong(0),
                    name = cursor.getString(1)
            )
        }
    }

    class FeedSettingsViewModel(feedId: Long) : ViewModel() {

        val feed = MediatorLiveData<Feed>()
        val feedTags = MediatorLiveData<List<FeedTag>>()

        var newUpdateMode: UpdateMode? = null

        var oldSelectedTagIds = LongArray(0)
        val newSelectedTagIds = MutableLiveData<LongArray>()

        init {
            val liveFeed = Feeds.liveQueryOne(Feeds.QueryRowCriteria(feedId), Feed.QueryHelper)
            feed.addSource(liveFeed) {
                feed.value = it
                feed.removeSource(liveFeed)
            }

            val liveFeedTags = FeedTags.liveQuery(FeedTags.QueryFeedTagsCriteria(feedId), FeedTag.QueryHelper)
            val liveTags = Tags.liveQuery(Tags.QueryAllTagsCriteria, Tag.QueryHelper)
            feedTags.addSource(liveFeedTags) {
                val selectedTagIds = LongArray(it.size) { index -> it[index].id }
                oldSelectedTagIds = selectedTagIds
                newSelectedTagIds.value = selectedTagIds

                feedTags.removeSource(liveFeedTags)
            }
            feedTags.addSource(liveTags) { updateFeedTags(it, newSelectedTagIds.value) }
            feedTags.addSource(newSelectedTagIds) { updateFeedTags(liveTags.value, it) }
        }

        private fun updateFeedTags(tags: List<Tag>?, selectedTagIds: LongArray?) {
            when {
                tags == null || selectedTagIds == null -> return
                selectedTagIds.isEmpty() -> feedTags.value = emptyList()
                else -> {
                    val newFeedTags = ArrayList<FeedTag>()
                    for (tag in tags) {
                        if (selectedTagIds.contains(tag.id)) {
                            newFeedTags.add(FeedTag(tag.id, tag.name))
                        }
                    }
                    this.feedTags.value = newFeedTags
                }
            }
        }

        class Factory(private val feedId: Long) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FeedSettingsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return FeedSettingsViewModel(feedId) as T
                }
                throw UnsupportedOperationException()
            }
        }

    }

}
