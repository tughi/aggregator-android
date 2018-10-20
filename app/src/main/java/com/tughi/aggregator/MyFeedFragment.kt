package com.tughi.aggregator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.tughi.aggregator.data.MyFeedUiEntriesGetter
import com.tughi.aggregator.data.UiEntriesGetter

class MyFeedFragment : EntryListFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = super.onCreateView(inflater, container, savedInstanceState)!!

        val toolbar = fragmentView.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            val activity = activity as MainActivity
            activity.openDrawer()
        }

        return fragmentView
    }

    override fun getLayout(): Int {
        return R.layout.my_feed_fragment
    }

    override fun getUiEntriesGetter(): UiEntriesGetter {
        return MyFeedUiEntriesGetter
    }

    companion object {
        fun newInstance(): MyFeedFragment {
            return MyFeedFragment()
        }
    }

}

