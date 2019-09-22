package com.tughi.aggregator.activities.tagsettings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.feedspicker.FeedsPickerActivity
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.FeedTags
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.widgets.DropDownButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TagSettingsActivity : AppActivity() {

    companion object {
        private const val EXTRA_TAG_ID = "tag_id"

        const val REQUEST_TAGGED_FEEDS = 1

        fun start(context: Context, tagId: Long?) {
            context.startActivity(
                    Intent(context, TagSettingsActivity::class.java).apply {
                        if (tagId != null) {
                            putExtra(EXTRA_TAG_ID, tagId)
                        }
                    }
            )
        }
    }

    private lateinit var viewModel: TagSettingsViewModel

    private val nameTextView by lazy { findViewById<TextView>(R.id.name) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = TagSettingsViewModel.Factory(intent.getLongExtra(EXTRA_TAG_ID, -1L).let { if (it == -1L) null else it })
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(TagSettingsViewModel::class.java)

        setContentView(R.layout.tag_settings_activity)

        findViewById<Button>(R.id.save).setOnClickListener { onSaveTag() }

        val feedsDropDownButton = findViewById<DropDownButton>(R.id.feeds)
        feedsDropDownButton.setOnClickListener {
            val taggedFeeds = viewModel.taggedFeeds.value ?: return@setOnClickListener
            val taggedFeedIds = LongArray(taggedFeeds.size) { taggedFeeds[it].id }
            FeedsPickerActivity.startForResult(this, REQUEST_TAGGED_FEEDS, taggedFeedIds, title = getString(R.string.tag_settings__tagged_feeds))
        }

        viewModel.tag.observe(this, Observer { tag ->
            if (tag != null) {
                invalidateOptionsMenu()

                nameTextView.text = tag.name
            }
        })

        viewModel.taggedFeeds.observe(this, Observer { feeds ->
            if (feeds != null && feeds.isNotEmpty()) {
                feedsDropDownButton.setText(feeds.joinToString())
            } else {
                feedsDropDownButton.setText(R.string.feed_settings__tags__none)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_TAGGED_FEEDS) {
            val selectedFeedIds = data?.extras?.getLongArray(FeedsPickerActivity.EXTRA_SELECTED_FEEDS)
            viewModel.newSelectedFeedIds.value = selectedFeedIds
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.tag_settings_activity, menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)

        if (menu != null) {
            val tag = viewModel.tag.value
            menu.findItem(R.id.delete)?.isVisible = tag?.deletable ?: false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.delete -> {
                viewModel.tag.value?.let { tag ->
                    DeleteTagDialogFragment.show(supportFragmentManager, tag.id, tag.name, true)
                }
                return true
            }
        }
        return false
    }

    private fun onSaveTag() {
        val tag = viewModel.tag.value
        val name = nameTextView.text.toString().trim()
        val newSelectedFeedIds = viewModel.newSelectedFeedIds.value ?: LongArray(0)
        val oldSelectedFeedIds = viewModel.oldSelectedFeedIds

        GlobalScope.launch {
            Database.transaction {
                val tagId = if (tag != null) {
                    Tags.update(
                            Tags.UpdateTagCriteria(tag.id),
                            Tags.NAME to name
                    )
                    tag.id
                } else {
                    Tags.insert(
                            Tags.NAME to name
                    )
                }

                for (feedId in newSelectedFeedIds) {
                    if (!oldSelectedFeedIds.contains(tagId)) {
                        FeedTags.insert(
                                FeedTags.FEED_ID to feedId,
                                FeedTags.TAG_ID to tagId,
                                FeedTags.TAG_TIME to System.currentTimeMillis()
                        )
                    }
                }

                for (feedId in oldSelectedFeedIds) {
                    if (!newSelectedFeedIds.contains(tagId)) {
                        FeedTags.delete(FeedTags.DeleteFeedTagCriteria(feedId, tagId))
                    }
                }
            }
        }

        finish()
    }

    class Feed(
            val id: Long,
            val title: String
    ) {
        override fun toString() = title

        object QueryHelper : Feeds.QueryHelper<Feed>(
                Feeds.ID,
                Feeds.CUSTOM_TITLE,
                Feeds.TITLE
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                    id = cursor.getLong(0),
                    title = cursor.getString(1) ?: cursor.getString(2)
            )
        }
    }

    class FeedTag(
            val feedId: Long
    ) {
        object QueryHelper : FeedTags.QueryHelper<FeedTag>(
                FeedTags.FEED_ID
        ) {
            override fun createRow(cursor: Cursor) = FeedTag(
                    feedId = cursor.getLong(0)
            )
        }
    }

    class Tag(val id: Long, val name: String, val deletable: Boolean) {
        object QueryHelper : Tags.QueryHelper<Tag>(
                Tags.ID,
                Tags.NAME,
                Tags.EDITABLE
        ) {
            override fun createRow(cursor: Cursor) = Tag(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    deletable = cursor.getInt(2) != 0
            )
        }
    }

    class TagSettingsViewModel(tagId: Long?) : ViewModel() {
        val tag = MediatorLiveData<Tag>()
        val taggedFeeds = MediatorLiveData<List<Feed>>()

        var oldSelectedFeedIds = LongArray(0)
        val newSelectedFeedIds = MutableLiveData<LongArray>()

        init {
            if (tagId != null) {
                val liveTag = Tags.liveQueryOne(Tags.QueryTagCriteria(tagId), Tag.QueryHelper)
                tag.addSource(liveTag) {
                    tag.value = it
                    tag.removeSource(liveTag)
                }

                val liveFeedTags = FeedTags.liveQuery(FeedTags.QueryTaggedFeedsCriteria(tagId), FeedTag.QueryHelper)
                taggedFeeds.addSource(liveFeedTags) {
                    val selectedFeedIds = LongArray(it.size) { index -> it[index].feedId }
                    oldSelectedFeedIds = selectedFeedIds
                    newSelectedFeedIds.value = selectedFeedIds

                    taggedFeeds.removeSource(liveFeedTags)
                }
            } else {
                newSelectedFeedIds.value = LongArray(0)
            }

            val liveFeeds = Feeds.liveQuery(Feeds.AllCriteria, Feed.QueryHelper)
            taggedFeeds.addSource(liveFeeds) { updateTaggedFeeds(it, newSelectedFeedIds.value) }
            taggedFeeds.addSource(newSelectedFeedIds) { updateTaggedFeeds(liveFeeds.value, it) }
        }

        private fun updateTaggedFeeds(feeds: List<Feed>?, selectedFeedIds: LongArray?) {
            when {
                feeds == null || selectedFeedIds == null -> return
                selectedFeedIds.isEmpty() -> taggedFeeds.value = emptyList()
                else -> {
                    val newTaggedFeeds = ArrayList<Feed>()
                    for (feed in feeds) {
                        if (selectedFeedIds.contains(feed.id)) {
                            newTaggedFeeds.add(feed)
                        }
                    }
                    this.taggedFeeds.value = newTaggedFeeds
                }
            }
        }

        class Factory(private val tagId: Long?) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TagSettingsViewModel::class.java)) {
                    return TagSettingsViewModel(tagId) as T
                }
                throw IllegalArgumentException("Unsupported model class: $modelClass")
            }
        }
    }


}
