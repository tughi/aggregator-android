package com.tughi.aggregator

import android.os.Bundle
import com.tughi.aggregator.data.FeedUiEntriesGetter
import com.tughi.aggregator.data.UiEntriesGetter

class FeedEntryListFragment : EntryListFragment() {

    override fun getUiEntriesGetter(): UiEntriesGetter {
        return FeedUiEntriesGetter(arguments!!.getLong(ARGUMENT_FEED_ID))
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

