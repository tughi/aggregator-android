package com.tughi.aggregator

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tughi.aggregator.viewmodels.FeedListViewModel

class FeedsFragment : Fragment() {

    companion object {
        fun newInstance(): FeedsFragment {
            return FeedsFragment();
        }
    }

    private lateinit var viewModel: FeedListViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProviders.of(this).get(FeedListViewModel::class.java)
        viewModel.feeds.observe(this, Observer { feeds ->
            Log.d("Aggregator", feeds.toString())
        })

        return inflater.inflate(R.layout.feeds_fragment, container, false)
    }

}