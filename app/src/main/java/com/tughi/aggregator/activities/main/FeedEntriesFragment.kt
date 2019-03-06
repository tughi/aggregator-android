package com.tughi.aggregator.activities.main

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.data.EntriesRepository
import com.tughi.aggregator.preferences.EntryListSettings

class FeedEntriesFragment : EntriesFragment() {

    private val feedId by lazy { arguments!!.getLong(ARGUMENT_FEED_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = FeedEntriesFragmentViewModel.Factory(feedId)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(FeedEntriesFragmentViewModel::class.java)
        viewModel.feed.observe(this, Observer { feed ->
            setTitle(feed.customTitle ?: feed.title)
        })
    }

    override val initialEntriesQuery: EntriesRepository.QueryCriteria
        get() = EntriesRepository.QueryCriteria.FeedEntries(feedId = feedId, sortOrder = EntryListSettings.entriesSortOrder)

    override fun onNavigationClick() {
        fragmentManager?.popBackStack()
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

