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
import com.tughi.aggregator.data.UiFeed

class FeedListAdapter : ListAdapter<UiFeed, FeedListAdapter.ViewHolder>(FeedDiffCallback()) {

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

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val favicon: ImageView = itemView.findViewById(R.id.favicon)
        val title: TextView = itemView.findViewById(R.id.title)
        val count: TextView = itemView.findViewById(R.id.count)
        val lastSuccessfulUpdateTextView: TextView = itemView.findViewById(R.id.last_successful_update)
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<UiFeed>() {

        override fun areItemsTheSame(oldFeed: UiFeed, newFeed: UiFeed): Boolean {
            return oldFeed.id == newFeed.id
        }

        override fun areContentsTheSame(oldFeed: UiFeed, newFeed: UiFeed): Boolean {
            return oldFeed == newFeed
        }

    }

}
