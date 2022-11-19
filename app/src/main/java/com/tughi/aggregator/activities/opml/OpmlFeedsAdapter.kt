package com.tughi.aggregator.activities.opml

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.feeds.OpmlFeed

internal class OpmlFeedsAdapter(val viewModel: OpmlFeedsViewModel) : RecyclerView.Adapter<OpmlFeedViewHolder>() {
    var feeds = emptyList<OpmlFeed>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = feeds.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpmlFeedViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.opml_import_list_item, parent, false)
        val viewHolder = OpmlFeedViewHolder(itemView)

        itemView.setOnClickListener {
            viewModel.toggleFeed(viewHolder.feed)
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: OpmlFeedViewHolder, position: Int) {
        holder.onBind(feeds[position])
    }
}
