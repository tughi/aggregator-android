package com.tughi.aggregator.activities.subscribe

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.updatemode.UpdateModeActivity
import com.tughi.aggregator.activities.updatemode.startUpdateModeActivity
import com.tughi.aggregator.activities.updatemode.toString
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FaviconUpdateScheduler
import com.tughi.aggregator.widgets.DropDownButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SubscribeFeedFragment : Fragment() {

    companion object {
        const val ARG_URL = "url"
        const val ARG_TITLE = "title"
        const val ARG_LINK = "link"

        private const val REQUEST_UPDATE_MODE = 1
    }

    private lateinit var viewModel: SubscribeFeedFragmentViewModel

    private lateinit var urlTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var updateModeTextView: DropDownButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.subscribe_feed_fragment, container, false)
        val arguments = requireArguments()

        viewModel = ViewModelProvider(this, SubscribeFeedFragmentViewModel.Factory())
                .get(SubscribeFeedFragmentViewModel::class.java)

        urlTextView = fragmentView.findViewById(R.id.url)
        urlTextView.text = arguments.getString(ARG_URL)

        titleTextView = fragmentView.findViewById(R.id.title)
        titleTextView.text = arguments.getString(ARG_TITLE)

        updateModeTextView = fragmentView.findViewById(R.id.update_mode)
        updateModeTextView.setOnClickListener {
            val currentUpdateMode = viewModel.updateMode.value ?: return@setOnClickListener
            startUpdateModeActivity(REQUEST_UPDATE_MODE, currentUpdateMode)
        }

        viewModel.updateMode.observe(viewLifecycleOwner, Observer {
            updateModeTextView.setText(it.toString(updateModeTextView.context))
        })

        return fragmentView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.subscribe_feed_fragment, menu)
    }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(R.string.title_add_feed)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                val arguments = requireArguments()
                val title = arguments.getString(ARG_TITLE)!!
                val customTitle = titleTextView.text.toString()
                val link = arguments.getString(ARG_LINK)

                GlobalScope.launch(Dispatchers.IO) {
                    val feedId = Feeds.insert(
                            Feeds.URL to urlTextView.text.toString(),
                            Feeds.TITLE to title,
                            Feeds.CUSTOM_TITLE to if (customTitle != title) customTitle else null,
                            Feeds.LINK to link,
                            Feeds.UPDATE_MODE to (viewModel.updateMode.value ?: DefaultUpdateMode).serialize()
                    )

                    launch {
                        AutoUpdateScheduler.scheduleFeed(feedId)
                    }

                    launch(Dispatchers.Main) {
                        FaviconUpdateScheduler.schedule(feedId)

                        activity?.finish()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_UPDATE_MODE && resultCode == Activity.RESULT_OK) {
            val serializedUpdateMode = data?.getStringExtra(UpdateModeActivity.EXTRA_UPDATE_MODE) ?: return
            viewModel.updateMode.value = UpdateMode.deserialize(serializedUpdateMode)
        }
    }

}
