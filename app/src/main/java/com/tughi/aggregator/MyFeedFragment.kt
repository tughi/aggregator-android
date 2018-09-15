package com.tughi.aggregator

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class MyFeedFragment : Fragment() {

    companion object {
        fun newInstance(): MyFeedFragment {
            return MyFeedFragment();
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.my_feed_fragment, container, false)
        return view
    }

}