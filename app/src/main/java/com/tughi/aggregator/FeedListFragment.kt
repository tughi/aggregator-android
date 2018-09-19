package com.tughi.aggregator

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.adapters.FeedListAdapter
import com.tughi.aggregator.viewmodels.FeedListViewModel

class FeedListFragment : Fragment() {

    companion object {
        fun newInstance(): FeedListFragment {
            return FeedListFragment()
        }
    }

    private lateinit var viewModel: FeedListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.feed_list_fragment, container, false)

        viewModel = ViewModelProviders.of(this).get(FeedListViewModel::class.java)

        val feeds = view.findViewById<RecyclerView>(R.id.feeds)
        feeds.adapter = FeedListAdapter().also { adapter ->
            viewModel.feeds.observe(this, Observer { list ->
                adapter.submitList(list)
            })
        }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater?.inflate(R.menu.feed_list_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.add ->
                Intent(activity, SubscribeActivity::class.java)
                        .apply { putExtra(SubscribeActivity.EXTRA_VIA_ACTION, true) }
                        .run { startActivity(this) }
            else ->
                return super.onOptionsItemSelected(item)
        }

        return true
    }

}