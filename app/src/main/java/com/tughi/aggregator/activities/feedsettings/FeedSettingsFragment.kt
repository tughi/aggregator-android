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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.feedtags.FeedTagsActivity
import com.tughi.aggregator.activities.updatemode.UpdateModeActivity
import com.tughi.aggregator.activities.updatemode.startUpdateModeActivity
import com.tughi.aggregator.activities.updatemode.toString
import com.tughi.aggregator.data.FeedTags
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FaviconUpdaterService
import com.tughi.aggregator.utilities.backupFeeds
import com.tughi.aggregator.widgets.makeClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FeedSettingsFragment : Fragment() {

    companion object {
        const val ARG_FEED_ID = "feed_id"

        const val REQUEST_UPDATE_MODE = 1
    }

    private lateinit var urlEditText: EditText
    private lateinit var titleEditText: EditText
    private lateinit var updateModeView: EditText
    private lateinit var tagsView: EditText

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

        updateModeView.makeClickable {
            val feed = viewModel.feed.value ?: return@makeClickable
            startUpdateModeActivity(REQUEST_UPDATE_MODE, viewModel.newUpdateMode ?: feed.updateMode)
        }


        tagsView.makeClickable {
            val feed = viewModel.feed.value ?: return@makeClickable
            context?.let { context ->
                FeedTagsActivity.start(context, feed.id)
            }
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
        if (requestCode == REQUEST_UPDATE_MODE && resultCode == Activity.RESULT_OK) {
            val serializedUpdateMode = data?.getStringExtra(UpdateModeActivity.EXTRA_UPDATE_MODE) ?: return
            viewModel.newUpdateMode = UpdateMode.deserialize(serializedUpdateMode).also { updateMode ->
                updateModeView.setText(updateMode.toString(updateModeView.context))
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

        viewModel.feed.value?.let { feed ->
            GlobalScope.launch {
                Feeds.update(
                        Feeds.UpdateRowCriteria(feed.id),
                        Feeds.URL to url,
                        Feeds.CUSTOM_TITLE to if (title.isEmpty() || title == feed.title) null else title,
                        Feeds.UPDATE_MODE to (updateMode ?: feed.updateMode).serialize()
                )

                if (updateMode != null && updateMode != feed.updateMode) {
                    AutoUpdateScheduler.scheduleFeed(feed.id)
                }

                GlobalScope.launch {
                    backupFeeds()
                }

                launch(Dispatchers.Main) {
                    FaviconUpdaterService.start(feed.id)

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
            val name: String
    ) {
        override fun toString(): String = name

        object QueryHelper : FeedTags.QueryHelper<FeedTag>(
                FeedTags.TAG_NAME
        ) {
            override fun createRow(cursor: Cursor) = FeedTag(
                    name = cursor.getString(0)
            )
        }
    }

    class FeedSettingsViewModel(feedId: Long) : ViewModel() {

        val feed: LiveData<Feed>
        val feedTags = FeedTags.liveQuery(FeedTags.QueryFeedTagsCriteria(feedId), FeedTag.QueryHelper)

        var newUpdateMode: UpdateMode? = null

        init {
            val liveFeed = MutableLiveData<Feed>()

            GlobalScope.launch {
                liveFeed.postValue(Feeds.queryOne(Feeds.QueryRowCriteria(feedId), Feed.QueryHelper))
            }

            feed = liveFeed
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
