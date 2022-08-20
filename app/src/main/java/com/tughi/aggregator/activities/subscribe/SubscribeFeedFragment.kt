package com.tughi.aggregator.activities.subscribe

import android.app.Activity
import android.content.Intent
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
import com.tughi.aggregator.activities.cleanupmode.startCleanupModeActivity
import com.tughi.aggregator.activities.cleanupmode.toString
import com.tughi.aggregator.activities.updatemode.UpdateModeActivity
import com.tughi.aggregator.activities.updatemode.startUpdateModeActivity
import com.tughi.aggregator.activities.updatemode.toString
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.DefaultCleanupMode
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.UpdateMode
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

        private const val REQUEST_UPDATE_MODE = 1
        private const val REQUEST_CLEANUP_MODE = 3
    }

    private lateinit var viewModel: SubscribeFeedFragmentViewModel

    private lateinit var urlTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var updateModeView: DropDownButton
    private lateinit var cleanupModeView: DropDownButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.subscribe_feed_fragment, container, false)
        val arguments = requireArguments()

        viewModel = ViewModelProvider(this, SubscribeFeedFragmentViewModel.Factory())
            .get(SubscribeFeedFragmentViewModel::class.java)

        urlTextView = fragmentView.findViewById(R.id.url)
        urlTextView.text = arguments.getString(ARG_URL)

        titleTextView = fragmentView.findViewById(R.id.title)
        titleTextView.text = arguments.getString(ARG_TITLE)

        updateModeView = fragmentView.findViewById(R.id.update_mode)
        updateModeView.setOnClickListener {
            val currentUpdateMode = viewModel.updateMode.value ?: return@setOnClickListener
            startUpdateModeActivity(REQUEST_UPDATE_MODE, currentUpdateMode)
        }

        viewModel.updateMode.observe(viewLifecycleOwner, {
            updateModeView.setText(it.toString(updateModeView.context))
        })

        cleanupModeView = fragmentView.findViewById(R.id.cleanup_mode)
        cleanupModeView.setOnClickListener {
            val currentCleanupMode = viewModel.cleanupMode.value ?: return@setOnClickListener
            startCleanupModeActivity(REQUEST_CLEANUP_MODE, currentCleanupMode)
        }

        viewModel.cleanupMode.observe(viewLifecycleOwner, {
            cleanupModeView.setText(it.toString(cleanupModeView.context))
        })

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_UPDATE_MODE -> {
                    val serializedUpdateMode = data?.getStringExtra(UpdateModeActivity.EXTRA_UPDATE_MODE) ?: return
                    viewModel.updateMode.value = UpdateMode.deserialize(serializedUpdateMode)
                }
                REQUEST_CLEANUP_MODE -> {
                    val serializedCleanupMode = data?.getStringExtra(CleanupModeActivity.EXTRA_CLEANUP_MODE) ?: return
                    viewModel.cleanupMode.value = CleanupMode.deserialize(serializedCleanupMode)
                }
            }
        }
    }

}
