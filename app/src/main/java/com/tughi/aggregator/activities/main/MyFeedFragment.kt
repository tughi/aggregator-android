package com.tughi.aggregator.activities.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tughi.aggregator.R
import com.tughi.aggregator.data.EntriesQuery
import com.tughi.aggregator.data.MyFeedEntriesQuery

class MyFeedFragment : EntriesFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = super.onCreateView(inflater, container, savedInstanceState)

        setTitle(R.string.title_my_feed)

        return fragmentView
    }

    override fun getEntriesQuery(): EntriesQuery {
        return MyFeedEntriesQuery(since = sessionTime)
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

