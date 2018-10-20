package com.tughi.aggregator

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.data.UiFeed
import com.tughi.aggregator.viewmodels.FeedListViewModel

class FeedListFragment : Fragment(), OnFeedClickedListener {

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

        feedsRecyclerView.adapter = FeedsAdapter(this).also { adapter ->
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

        val toolbar = fragmentView.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            val activity = activity as MainActivity
            activity.openDrawer()
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

    override fun onFeedClicked(feed: UiFeed) {
        fragmentManager!!.beginTransaction()
                .replace(id, FeedEntryListFragment.newInstance(feedId = feed.id))
                .addToBackStack(null)
                .commit()
    }

}

private class FeedsAdapter(private val listener: OnFeedClickedListener) : ListAdapter<UiFeed, FeedViewHolder>(FeedsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.feed_list_item, parent, false)
        return FeedViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val feed = getItem(position)

        holder.feed = feed
        holder.favicon.setImageResource(R.drawable.favicon_placeholder)
        holder.title.text = feed.title
        if (feed.entryCount == 0) {
            holder.count.text = ""
            holder.count.visibility = View.INVISIBLE
        } else {
            holder.count.text = feed.entryCount.toString()
            holder.count.visibility = View.VISIBLE
        }
        if (feed.updateTime == 0L) {
            holder.lastUpdateTime.setText(R.string.last_update_time__never)
        } else {
            val context = holder.itemView.context
            DateUtils.getRelativeDateTimeString(context, feed.updateTime, DateUtils.DAY_IN_MILLIS, DateUtils.DAY_IN_MILLIS, 0).let {
                holder.lastUpdateTime.text = context.getString(R.string.last_update_time__since, it)
            }
        }
    }

}

private class FeedsDiffCallback : DiffUtil.ItemCallback<UiFeed>() {

    override fun areItemsTheSame(oldFeed: UiFeed, newFeed: UiFeed): Boolean {
        return oldFeed.id == newFeed.id
    }

    override fun areContentsTheSame(oldFeed: UiFeed, newFeed: UiFeed): Boolean {
        return oldFeed == newFeed
    }

}

private class FeedViewHolder(itemView: View, private val listener: OnFeedClickedListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    val favicon: ImageView = itemView.findViewById(R.id.favicon)
    val title: TextView = itemView.findViewById(R.id.title)
    val count: TextView = itemView.findViewById(R.id.count)
    val lastUpdateTime: TextView = itemView.findViewById(R.id.last_update_time)

    lateinit var feed: UiFeed

    init {
        itemView.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        listener.onFeedClicked(feed)
    }

}

private interface OnFeedClickedListener {
    fun onFeedClicked(feed: UiFeed)
}
