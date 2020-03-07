package com.tughi.aggregator.activities.main

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.EntriesQueryCriteria
import com.tughi.aggregator.data.FeedEntriesQueryCriteria
import com.tughi.aggregator.preferences.EntryListSettings

class FeedEntriesFragment : EntriesFragment() {

    private val feedId by lazy { requireArguments().getLong(ARGUMENT_FEED_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = FeedEntriesFragmentViewModel.Factory(feedId)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(FeedEntriesFragmentViewModel::class.java)
        viewModel.feed.observe(viewLifecycleOwner, Observer { feed ->
            if (feed != null) {
                setTitle(feed.title)
            }
        })
    }

    override val initialQueryCriteria: EntriesQueryCriteria
        get() = FeedEntriesQueryCriteria(feedId = feedId, sessionTime = System.currentTimeMillis(), showRead = EntryListSettings.showReadEntries, sortOrder = EntryListSettings.entriesSortOrder)

    override fun onNavigationClick() {
        parentFragmentManager.popBackStack()
    }

    companion object {
        const val ARGUMENT_FEED_ID = "feed_id"

        fun newInstance(feedId: Long): FeedEntriesFragment {
            return FeedEntriesFragment().also {
                it.arguments = Bundle().apply {
                    putLong(ARGUMENT_FEED_ID, feedId)
                }
            }
        }
    }

}

