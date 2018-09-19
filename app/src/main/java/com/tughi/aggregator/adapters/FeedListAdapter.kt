package com.tughi.aggregator.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.data.Feed

class FeedListAdapter : ListAdapter<Feed, FeedListAdapter.ViewHolder>(FeedDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.feed_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feed = getItem(position)

        holder.favicon.setImageResource(R.drawable.favicon_placeholder)
        holder.title.text = feed.title
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val favicon: ImageView = itemView.findViewById(R.id.favicon)
        val title: TextView = itemView.findViewById(R.id.title)
        val count: TextView = itemView.findViewById(R.id.count)

        init {
            count.visibility = View.GONE
        }
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<Feed>() {

        override fun areItemsTheSame(oldFeed: Feed, newFeed: Feed): Boolean {
            return oldFeed.id == newFeed.id
        }

        override fun areContentsTheSame(oldFeed: Feed, newFeed: Feed): Boolean {
            return oldFeed == newFeed
        }

    }

}