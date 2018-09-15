package com.tughi.aggregator

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class FeedsFragment : Fragment() {

    companion object {
        fun newInstance(): FeedsFragment {
            return FeedsFragment();
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.feeds_fragment, container, false)
    }

}