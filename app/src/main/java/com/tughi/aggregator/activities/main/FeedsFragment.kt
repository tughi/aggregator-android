package com.tughi.aggregator.activities.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.opml.OpmlExportActivity
import com.tughi.aggregator.activities.subscribe.SubscribeActivity
import com.tughi.aggregator.activities.updatesettings.UpdateSettingsActivity
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FeedUpdateHelper
import kotlinx.coroutines.launch

class FeedListFragment : Fragment(), FeedsFragmentFeedAdapterListener {

    private lateinit var viewModel: FeedsFragmentViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.feeds_fragment, container, false)

        viewModel = ViewModelProvider(this).get(FeedsFragmentViewModel::class.java)

        val feedsRecyclerView = fragmentView.findViewById<RecyclerView>(R.id.feeds)
        val emptyView = fragmentView.findViewById<View>(R.id.empty)
        val progressBar = fragmentView.findViewById<View>(R.id.progress)

        feedsRecyclerView.adapter = FeedsFragmentFeedAdapter(this).also { adapter ->
            viewModel.feeds.observe(viewLifecycleOwner) { feeds ->
                adapter.submitList(feeds)

                progressBar.visibility = View.GONE
                if (feeds?.size == 0) {
                    emptyView.visibility = View.VISIBLE
                    feedsRecyclerView.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    feedsRecyclerView.visibility = View.VISIBLE
                }
            }
        }

        fragmentView.findViewById<Button>(R.id.add).setOnClickListener {
            SubscribeActivity.start(it.context)
        }

        val toolbar = fragmentView.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            val activity = activity as MainActivity
            activity.openDrawer()
        }
        toolbar.inflateMenu(R.menu.feed_list_fragment)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add -> {
                    SubscribeActivity.start(requireActivity(), viaAction = true)
                }
                R.id.export -> {
                    startActivity(Intent(requireContext(), OpmlExportActivity::class.java))
                }
                R.id.update_settings -> {
                    startActivity(Intent(requireContext(), UpdateSettingsActivity::class.java))
                }
                else -> {
                    return@setOnMenuItemClickListener false
                }
            }

            return@setOnMenuItemClickListener true
        }

        return fragmentView
    }

    override fun onFeedClicked(feed: FeedsFragmentViewModel.Feed) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, 0, 0, R.anim.fade_out)
            .add(id, FeedEntriesFragment.newInstance(feedId = feed.id), TAG)
            .addToBackStack(null)
            .commit()
    }

    override fun onToggleFeed(feed: FeedsFragmentViewModel.Feed) {
        viewModel.toggleFeed(feed)
    }

    override fun onUpdateFeed(feed: FeedsFragmentViewModel.Feed) {
        contentScope.launch {
            FeedUpdateHelper.updateFeed(feed.id)

            AutoUpdateScheduler.schedule()
        }
    }

    companion object {
        const val TAG = "feed"

        fun newInstance(): FeedListFragment {
            return FeedListFragment()
        }
    }

}
