package com.tughi.aggregator

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.data.UiFeed
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
        val fragmentView = inflater.inflate(R.layout.feed_list_fragment, container, false)

        viewModel = ViewModelProviders.of(this).get(FeedListViewModel::class.java)

        val feedsRecyclerView = fragmentView.findViewById<RecyclerView>(R.id.feeds)
        val emptyView = fragmentView.findViewById<View>(R.id.empty)
        val progressBar = fragmentView.findViewById<View>(R.id.progress)

        feedsRecyclerView.adapter = Adapter().also { adapter ->
            viewModel.feeds.observe(this, Observer { feeds ->
                adapter.submitList(feeds)

                progressBar.visibility = View.GONE
                if (feeds.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    feedsRecyclerView.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    feedsRecyclerView.visibility = View.VISIBLE
                }
            })
        }

        fragmentView.findViewById<Button>(R.id.add).setOnClickListener {
            startActivity(Intent(activity, SubscribeActivity::class.java))
        }

        return fragmentView
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

private class Adapter : ListAdapter<UiFeed, ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.feed_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feed = getItem(position)

        holder.favicon.setImageResource(R.drawable.favicon_placeholder)
        holder.title.text = feed.title
        holder.count.text = feed.entryCount.toString()
        holder.count.visibility = if (feed.entryCount == 0) View.GONE else View.VISIBLE
        holder.lastSuccessfulUpdateTextView.setText(R.string.last_successful_update__never)
    }

}

private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val favicon: ImageView = itemView.findViewById(R.id.favicon)
    val title: TextView = itemView.findViewById(R.id.title)
    val count: TextView = itemView.findViewById(R.id.count)
    val lastSuccessfulUpdateTextView: TextView = itemView.findViewById(R.id.last_successful_update)

}

private class DiffCallback : DiffUtil.ItemCallback<UiFeed>() {

    override fun areItemsTheSame(oldFeed: UiFeed, newFeed: UiFeed): Boolean {
        return oldFeed.id == newFeed.id
    }

    override fun areContentsTheSame(oldFeed: UiFeed, newFeed: UiFeed): Boolean {
        return oldFeed == newFeed
    }

}

