package com.tughi.aggregator

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.data.FeedUiEntriesGetter
import com.tughi.aggregator.data.UiEntriesGetter
import com.tughi.aggregator.viewmodels.FeedViewModel

class FeedEntryListFragment : EntryListFragment() {

    private val feedId by lazy { arguments!!.getLong(ARGUMENT_FEED_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = FeedViewModel.Factory(feedId)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(FeedViewModel::class.java)
        viewModel.feed.observe(this, Observer { feed ->
            setTitle(feed.title)
        })
    }

    override fun getUiEntriesGetter(): UiEntriesGetter {
        return FeedUiEntriesGetter(feedId, since = sessionTime)
    }

    override fun onNavigationClick() {
        fragmentManager?.popBackStack()
    }

    companion object {
        const val ARGUMENT_FEED_ID = "feed_id"

        fun newInstance(feedId: Long): FeedEntryListFragment {
            return FeedEntryListFragment().also {
                it.arguments = Bundle().apply {
                    putLong(ARGUMENT_FEED_ID, feedId)
                }
            }
        }
    }

}

