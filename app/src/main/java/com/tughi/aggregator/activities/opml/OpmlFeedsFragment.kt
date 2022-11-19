package com.tughi.aggregator.activities.opml

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R

class OpmlFeedsFragment : Fragment(R.layout.opml_fragment) {
    private lateinit var viewModel: OpmlFeedsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.opml_feeds_fragment, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.invert_selection) {
                    viewModel.toggleAllFeeds()
                    return true
                }
                return false
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        viewModel = ViewModelProvider(activity).get()

        val feedsAdapter = OpmlFeedsAdapter(viewModel)

        view.findViewById<RecyclerView>(R.id.feeds).adapter = feedsAdapter

        viewModel.feeds.observe(activity) { feeds ->
            feedsAdapter.feeds = feeds
        }
    }
}
