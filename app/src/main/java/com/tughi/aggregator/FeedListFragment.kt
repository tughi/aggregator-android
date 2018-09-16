package com.tughi.aggregator

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tughi.aggregator.adapters.FeedListAdapter
import com.tughi.aggregator.viewmodels.FeedListViewModel
import kotlinx.android.synthetic.main.feed_list_fragment.view.*

class FeedListFragment : Fragment() {

    companion object {
        fun newInstance(): FeedListFragment {
            return FeedListFragment();
        }
    }

    private lateinit var viewModel: FeedListViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.feed_list_fragment, container, false)

        viewModel = ViewModelProviders.of(this).get(FeedListViewModel::class.java)

        view.feeds.adapter = FeedListAdapter().also { adapter ->
            viewModel.feeds.observe(this, Observer { list ->
                adapter.submitList(list)
            })
        }

        return view
    }

}