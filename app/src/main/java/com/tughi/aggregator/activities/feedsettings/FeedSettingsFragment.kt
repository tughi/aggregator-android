package com.tughi.aggregator.activities.feedsettings

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.cleanupmode.CleanupModeActivity
import com.tughi.aggregator.activities.cleanupmode.toString
import com.tughi.aggregator.activities.feedentrytagrules.FeedEntryTagRulesActivity
import com.tughi.aggregator.activities.updatemode.UpdateModeActivity
import com.tughi.aggregator.activities.updatemode.toString
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.FeedEntryTagRulesQueryCriteria
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FaviconUpdateScheduler
import com.tughi.aggregator.utilities.backupFeeds
import com.tughi.aggregator.widgets.DropDownButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedSettingsFragment : Fragment() {

    companion object {
        const val ARG_FEED_ID = "feed_id"

        const val REQUEST_ENTRY_RULES = 4
    }

    private lateinit var urlEditText: EditText
    private lateinit var titleEditText: EditText
    private lateinit var updateModeView: DropDownButton
    private lateinit var cleanupModeView: DropDownButton
    private lateinit var entryTagRulesView: DropDownButton

    private lateinit var viewModel: FeedSettingsViewModel

    private val requestCleanupMode = registerForActivityResult(CleanupModeActivity.PickCleanupMode()) { cleanupMode ->
        if (cleanupMode != null) {
            viewModel.newCleanupMode = cleanupMode
            cleanupModeView.setText(cleanupMode.toString(requireContext()))
        }
    }

    private val requestUpdateMode = registerForActivityResult(UpdateModeActivity.PickUpdateMode()) { updateMode ->
        if (updateMode != null) {
            viewModel.newUpdateMode = updateMode
            updateModeView.setText(updateMode.toString(requireContext()))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = requireActivity()

        activity.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.feed_settings_fragment, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.unsubscribe) {
                    onUnsubscribe()
                    return true
                }
                return false
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.feed_settings_fragment, container, false)

        urlEditText = fragmentView.findViewById(R.id.url)
        titleEditText = fragmentView.findViewById(R.id.title)
        updateModeView = fragmentView.findViewById(R.id.update_mode)
        cleanupModeView = fragmentView.findViewById(R.id.cleanup_mode)
        entryTagRulesView = fragmentView.findViewById(R.id.entry_tag_rules)

        updateModeView.setOnClickListener {
            val feed = viewModel.feed.value ?: return@setOnClickListener
            requestUpdateMode.launch(UpdateModeActivity.PickUpdateModeRequest(viewModel.newUpdateMode ?: feed.updateMode))
        }

        cleanupModeView.setOnClickListener {
            val feed = viewModel.feed.value ?: return@setOnClickListener
            requestCleanupMode.launch(CleanupModeActivity.PickCleanupModeRequest(viewModel.newCleanupMode ?: feed.cleanupMode))
        }

        entryTagRulesView.setOnClickListener {
            val feed = viewModel.feed.value ?: return@setOnClickListener
            FeedEntryTagRulesActivity.startForResult(this, REQUEST_ENTRY_RULES, feedId = feed.id)
        }

        val feedId = requireArguments().getLong(ARG_FEED_ID)
        viewModel = ViewModelProvider(this, FeedSettingsViewModel.Factory(feedId))[FeedSettingsViewModel::class.java]

        viewModel.feed.observe(viewLifecycleOwner) { feed ->
            if (feed != null) {
                urlEditText.setText(feed.url)
                titleEditText.setText(feed.customTitle ?: feed.title)
                updateModeView.setText(feed.updateMode.toString(updateModeView.context))
                cleanupModeView.setText(feed.cleanupMode.toString(cleanupModeView.context))
            }
        }

        viewModel.entryTagRuleCount.observe(viewLifecycleOwner) { count ->
            entryTagRulesView.setText(resources.getQuantityString(R.plurals.feed_settings__entry_tag_rules, count, count))
        }

        fragmentView.findViewById<View>(R.id.save).setOnClickListener {
            onSave()
        }

        return fragmentView
    }

    private fun onSave() {
        val url = urlEditText.text.toString().trim()
        val title = titleEditText.text.toString().trim()
        val cleanupMode = viewModel.newCleanupMode
        val updateMode = viewModel.newUpdateMode

        viewModel.feed.value?.let { feed ->
            val faviconUrl = if (url == feed.url) feed.faviconUrl else null

            contentScope.launch {
                Database.transaction {
                    Feeds.update(
                        Feeds.UpdateRowCriteria(feed.id),
                        Feeds.URL to url,
                        Feeds.CUSTOM_TITLE to if (title.isEmpty() || title == feed.title) null else title,
                        Feeds.CLEANUP_MODE to (cleanupMode ?: feed.cleanupMode).serialize(),
                        Feeds.UPDATE_MODE to (updateMode ?: feed.updateMode).serialize(),
                        Feeds.FAVICON_URL to faviconUrl,
                        // clear state to refresh feed on next update
                        Feeds.HTTP_ETAG to null,
                        Feeds.HTTP_LAST_MODIFIED to null,
                    )
                }

                if (updateMode != null && updateMode != feed.updateMode) {
                    AutoUpdateScheduler.scheduleFeed(feed.id)
                }

                launch {
                    backupFeeds()
                }

                launch(Dispatchers.Main) {
                    if (faviconUrl == null) {
                        FaviconUpdateScheduler.schedule()
                    }

                    activity?.finish()
                }
            }
        }
    }

    private fun onUnsubscribe() {
        viewModel.feed.value?.let {
            UnsubscribeDialogFragment.show(parentFragmentManager, it.id, it.customTitle ?: it.title, true)
        }
    }

    class Feed(
        val id: Long,
        val url: String,
        val title: String,
        val customTitle: String?,
        val cleanupMode: CleanupMode,
        val updateMode: UpdateMode,
        val faviconUrl: String?
    ) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
            Feeds.ID,
            Feeds.URL,
            Feeds.TITLE,
            Feeds.CUSTOM_TITLE,
            Feeds.CLEANUP_MODE,
            Feeds.UPDATE_MODE,
            Feeds.FAVICON_URL
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                id = cursor.getLong(0),
                url = cursor.getString(1),
                title = cursor.getString(2),
                customTitle = cursor.getString(3),
                cleanupMode = CleanupMode.deserialize(cursor.getString(4)),
                updateMode = UpdateMode.deserialize(cursor.getString(5)),
                faviconUrl = cursor.getString(6)
            )
        }
    }

    class EntryTagRule(
        val id: Long
    ) {
        object QueryHelper : EntryTagRules.QueryHelper<EntryTagRule>(
            EntryTagRules.ID
        ) {
            override fun createRow(cursor: Cursor) = EntryTagRule(
                id = cursor.getLong(0)
            )
        }
    }

    class FeedSettingsViewModel(feedId: Long) : ViewModel() {

        val feed = MediatorLiveData<Feed>()

        var newUpdateMode: UpdateMode? = null
        var newCleanupMode: CleanupMode? = null

        val entryTagRuleCount = EntryTagRules.liveQueryCount(FeedEntryTagRulesQueryCriteria(feedId), EntryTagRule.QueryHelper)

        init {
            val liveFeed = Feeds.liveQueryOne(Feeds.QueryRowCriteria(feedId), Feed.QueryHelper)
            feed.addSource(liveFeed) {
                feed.value = it
                feed.removeSource(liveFeed)
            }
        }

        class Factory(private val feedId: Long) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FeedSettingsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return FeedSettingsViewModel(feedId) as T
                }
                throw UnsupportedOperationException()
            }
        }

    }

}
