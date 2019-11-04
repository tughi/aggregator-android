package com.tughi.aggregator.activities.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.myfeedsettings.MyFeedSettingsActivity
import com.tughi.aggregator.data.EntriesQueryCriteria
import com.tughi.aggregator.data.MyFeedEntriesQueryCriteria
import com.tughi.aggregator.preferences.EntryListSettings

class MyFeedFragment : EntriesFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = super.onCreateView(inflater, container, savedInstanceState) ?: return null

        setTitle(R.string.title_my_feed)

        fragmentView.findViewById<Toolbar>(R.id.toolbar)
                .inflateMenu(R.menu.my_feed_fragment)

        return fragmentView
    }

    override val initialQueryCriteria: EntriesQueryCriteria
        get() = MyFeedEntriesQueryCriteria(sessionTime = System.currentTimeMillis(), sortOrder = EntryListSettings.entriesSortOrder)

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.settings -> {
                context?.let { MyFeedSettingsActivity.start(it) }
            }
            else -> {
                return super.onMenuItemClick(item)
            }
        }
        return true
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

