package com.tughi.aggregator.activities.opml

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.feeds.OpmlFeed

internal class OpmlFeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val checkbox: ImageView = itemView.findViewById(R.id.checkbox)
    val title: TextView = itemView.findViewById(R.id.title)
    val url: TextView = itemView.findViewById(R.id.url)

    lateinit var feed: OpmlFeed

    fun onBind(feed: OpmlFeed) {
        this.feed = feed

        if (feed.selected) {
            checkbox.setImageResource(R.drawable.check_box_checked)
        } else {
            checkbox.setImageResource(R.drawable.check_box_unchecked)
        }

        title.isEnabled = !feed.aggregated
        title.text = feed.customTitle ?: feed.title

        url.isEnabled = !feed.aggregated
        url.text = feed.url
    }
}
