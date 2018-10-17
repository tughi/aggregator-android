package com.tughi.aggregator

import com.tughi.aggregator.data.MyFeedUiEntriesGetter
import com.tughi.aggregator.data.UiEntriesGetter

class MyFeedFragment : EntryListFragment() {

    override fun getUiEntriesGetter(): UiEntriesGetter {
        return MyFeedUiEntriesGetter
    }

    companion object {
        fun newInstance(): MyFeedFragment {
            return MyFeedFragment()
        }
    }

}

