package com.tughi.aggregator

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.tughi.aggregator.services.FeedUpdater
import com.tughi.aggregator.utilities.Favicons
import com.tughi.aggregator.viewmodels.FeedListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FeedListFragment : Fragment(), OnFeedClickedListener {

    private lateinit var viewModel: FeedListViewModel

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
        toolbar.inflateMenu(R.menu.feed_list_fragment)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add -> {
                    startActivity(Intent(activity, SubscribeActivity::class.java).putExtra(SubscribeActivity.EXTRA_VIA_ACTION, true))
                }
                R.id.update_settings -> {
                    startActivity(Intent(activity, UpdateSettingsActivity::class.java))
                }
                else -> {
                    return@setOnMenuItemClickListener false
                }
            }

            return@setOnMenuItemClickListener true
        }

        return fragmentView
    }

    override fun onFeedClicked(feed: UiFeed) {
        fragmentManager!!.beginTransaction()
                .setCustomAnimations(R.anim.slide_in, 0, 0, R.anim.fade_out)
                .add(id, FeedEntryListFragment.newInstance(feedId = feed.id), TAG)
                .addToBackStack(null)
                .commit()
    }

    override fun onToggleFeed(feed: UiFeed) {
        viewModel.toggleFeed(feed)
    }

    override fun onUpdateFeed(feed: UiFeed) {
        GlobalScope.launch(Dispatchers.IO) {
            FeedUpdater.updateFeed(feed.id)
        }
    }

    companion object {
        const val TAG = "feed"

        fun newInstance(): FeedListFragment {
            return FeedListFragment()
        }
    }

}

private class FeedsAdapter(private val listener: OnFeedClickedListener) : ListAdapter<UiFeed, FeedListItemViewHolder>(FeedsDiffCallback()) {

    override fun getItemViewType(position: Int): Int = when (getItem(position).expanded) {
        true -> R.layout.feed_list_item_expanded
        false -> R.layout.feed_list_item_collapsed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedListItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        val viewHolder = when (viewType) {
            R.layout.feed_list_item_expanded -> ExpandedFeedViewHolder(view)
            else -> CollapsedFeedViewHolder(view)
        }

        viewHolder.itemView.setOnClickListener {
            listener.onFeedClicked(viewHolder.feed)
        }
        viewHolder.toggle.setOnClickListener {
            listener.onToggleFeed(viewHolder.feed)
        }

        if (viewHolder is ExpandedFeedViewHolder) {
            viewHolder.updateButton.setOnClickListener {
                listener.onUpdateFeed(viewHolder.feed)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: FeedListItemViewHolder, position: Int) {
        holder.onBind(getItem(position))
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

private abstract class FeedListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val favicon: ImageView = itemView.findViewById(R.id.favicon)
    val title: TextView = itemView.findViewById(R.id.title)
    val count: TextView = itemView.findViewById(R.id.count)
    val toggle: View = itemView.findViewById(R.id.toggle)

    lateinit var feed: UiFeed

    open fun onBind(feed: UiFeed) {
        this.feed = feed

        title.text = feed.title
        if (feed.unreadEntryCount == 0) {
            count.text = ""
            count.visibility = View.INVISIBLE
        } else {
            count.text = feed.unreadEntryCount.toString()
            count.visibility = View.VISIBLE
        }

        if (feed.updating) {
            favicon.setImageResource(R.drawable.action_refresh)
        } else {
            Favicons.load(feed.id, feed.faviconUrl, favicon)
        }
    }

}

private class CollapsedFeedViewHolder(itemView: View) : FeedListItemViewHolder(itemView)

private class ExpandedFeedViewHolder(itemView: View) : FeedListItemViewHolder(itemView) {

    val lastUpdateTime: TextView = itemView.findViewById(R.id.last_update_time)
    val nextUpdateTime: TextView = itemView.findViewById(R.id.next_update_time)
    val updateMode: TextView = itemView.findViewById(R.id.update_mode)
    val settingsButton: View = itemView.findViewById(R.id.settings)
    val updateButton: View = itemView.findViewById(R.id.update)

    init {
        settingsButton.setOnClickListener {
            val context = it.context
            context.startActivity(
                    Intent(context, FeedSettingsActivity::class.java)
                            .putExtra(FeedSettingsActivity.EXTRA_FEED_ID, feed.id)
            )
        }
    }

    override fun onBind(feed: UiFeed) {
        super.onBind(feed)

        val context = itemView.context
        if (feed.lastUpdateTime == 0L) {
            lastUpdateTime.setText(R.string.last_update_time__never)
        } else {
            DateUtils.getRelativeDateTimeString(context, feed.lastUpdateTime, DateUtils.DAY_IN_MILLIS, DateUtils.DAY_IN_MILLIS, 0).let {
                lastUpdateTime.text = context.getString(R.string.last_update_time__since, it)
            }
        }
        DateUtils.getRelativeDateTimeString(context, feed.nextUpdateTime, DateUtils.DAY_IN_MILLIS, DateUtils.DAY_IN_MILLIS, 0).let {
            nextUpdateTime.text = context.getString(R.string.next_update_time, it)
        }
        updateMode.setText(R.string.update_mode__default)

        updateButton.isEnabled = !feed.updating
    }
}

private interface OnFeedClickedListener {

    fun onFeedClicked(feed: UiFeed)

    fun onToggleFeed(feed: UiFeed)

    fun onUpdateFeed(feed: UiFeed)

}
