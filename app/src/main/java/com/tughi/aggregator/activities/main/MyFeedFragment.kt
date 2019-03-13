package com.tughi.aggregator.activities.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tughi.aggregator.R
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.preferences.EntryListSettings

class MyFeedFragment : EntriesFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = super.onCreateView(inflater, container, savedInstanceState)

        setTitle(R.string.title_my_feed)

        return fragmentView
    }

    override val initialQueryCriteria: Entries.EntriesQueryCriteria
        get() = Entries.MyFeedEntriesQueryCriteria(sortOrder = EntryListSettings.entriesSortOrder)

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

