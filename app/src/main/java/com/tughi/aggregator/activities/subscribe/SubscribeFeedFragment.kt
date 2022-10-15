package com.tughi.aggregator.activities.subscribe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.cleanupmode.CleanupModeActivity
import com.tughi.aggregator.activities.cleanupmode.toString
import com.tughi.aggregator.activities.updatemode.UpdateModeActivity
import com.tughi.aggregator.activities.updatemode.toString
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.DefaultCleanupMode
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FaviconUpdateScheduler
import com.tughi.aggregator.utilities.backupFeeds
import com.tughi.aggregator.widgets.DropDownButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SubscribeFeedFragment : Fragment() {

    companion object {
        const val ARG_URL = "url"
        const val ARG_TITLE = "title"
        const val ARG_LINK = "link"
    }

    private lateinit var viewModel: SubscribeFeedFragmentViewModel

    private lateinit var urlTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var updateModeView: DropDownButton
    private lateinit var cleanupModeView: DropDownButton

    private val requestCleanupMode = registerForActivityResult(CleanupModeActivity.PickCleanupMode()) { cleanupMode ->
        if (cleanupMode != null) {
            viewModel.cleanupMode.value = cleanupMode
        }
    }

    private val requestUpdateMode = registerForActivityResult(UpdateModeActivity.PickUpdateMode()) { updateMode ->
        if (updateMode != null) {
            viewModel.updateMode.value = updateMode
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.subscribe_feed_fragment, container, false)
        val arguments = requireArguments()

        viewModel = ViewModelProvider(this, SubscribeFeedFragmentViewModel.Factory())[SubscribeFeedFragmentViewModel::class.java]

        urlTextView = fragmentView.findViewById(R.id.url)
        urlTextView.text = arguments.getString(ARG_URL)

        titleTextView = fragmentView.findViewById(R.id.title)
        titleTextView.text = arguments.getString(ARG_TITLE)

        updateModeView = fragmentView.findViewById(R.id.update_mode)
        updateModeView.setOnClickListener {
            val currentUpdateMode = viewModel.updateMode.value ?: return@setOnClickListener
            requestUpdateMode.launch(UpdateModeActivity.PickUpdateModeRequest(currentUpdateMode))
        }

        viewModel.updateMode.observe(viewLifecycleOwner) {
            updateModeView.setText(it.toString(updateModeView.context))
        }

        cleanupModeView = fragmentView.findViewById(R.id.cleanup_mode)
        cleanupModeView.setOnClickListener {
            val currentCleanupMode = viewModel.cleanupMode.value ?: return@setOnClickListener
            requestCleanupMode.launch(CleanupModeActivity.PickCleanupModeRequest(currentCleanupMode))
        }

        viewModel.cleanupMode.observe(viewLifecycleOwner) {
            cleanupModeView.setText(it.toString(cleanupModeView.context))
        }

        val subscribeButton = fragmentView.findViewById<Button>(R.id.subscribe)
        subscribeButton.setOnClickListener {
            val title = arguments.getString(ARG_TITLE)!!
            val customTitle = titleTextView.text.toString()
            val link = arguments.getString(ARG_LINK)

            contentScope.launch {
                val feedId = Feeds.insert(
                    Feeds.URL to urlTextView.text.toString(),
                    Feeds.TITLE to title,
                    Feeds.CUSTOM_TITLE to if (customTitle != title) customTitle else null,
                    Feeds.LINK to link,
                    Feeds.UPDATE_MODE to (viewModel.updateMode.value ?: DefaultUpdateMode).serialize(),
                    Feeds.CLEANUP_MODE to (viewModel.cleanupMode.value ?: DefaultCleanupMode).serialize()
                )

                launch {
                    AutoUpdateScheduler.scheduleFeed(feedId)

                    backupFeeds()
                }

                launch(Dispatchers.Main) {
                    FaviconUpdateScheduler.schedule()

                    activity?.finish()
                }
            }
        }

        return fragmentView
    }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(R.string.title_add_feed)
    }

}
