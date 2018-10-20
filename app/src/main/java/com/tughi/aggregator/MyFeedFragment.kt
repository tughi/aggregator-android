package com.tughi.aggregator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tughi.aggregator.data.MyFeedUiEntriesGetter
import com.tughi.aggregator.data.UiEntriesGetter

class MyFeedFragment : EntryListFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = super.onCreateView(inflater, container, savedInstanceState)

        setTitle(R.string.title_my_feed)

        return fragmentView
    }

    override fun getUiEntriesGetter(): UiEntriesGetter {
        return MyFeedUiEntriesGetter
    }

    override fun onNavigationClick() {
        val activity = activity as MainActivity
        activity.openDrawer()
    }

    companion object {
        fun newInstance(): MyFeedFragment {
            return MyFeedFragment()
        }
    }

}

