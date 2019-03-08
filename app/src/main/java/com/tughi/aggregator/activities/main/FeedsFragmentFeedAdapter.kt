package com.tughi.aggregator.activities.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.tughi.aggregator.R

internal class FeedsFragmentFeedAdapter(private val listener: FeedsFragmentFeedAdapterListener) : ListAdapter<FeedsFragmentViewModel.Feed, FeedsFragmentFeedViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int = when (getItem(position).expanded) {
        true -> R.layout.feeds_item_expanded
        false -> R.layout.feeds_item_collapsed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedsFragmentFeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        val viewHolder = when (viewType) {
            R.layout.feeds_item_expanded -> FeedsFragmentExpandedFeedViewHolder(view)
            else -> FeedsFragmentCollapsedFeedViewHolder(view)
        }

        viewHolder.itemView.setOnClickListener {
            listener.onFeedClicked(viewHolder.feed)
        }
        viewHolder.toggle.setOnClickListener {
            listener.onToggleFeed(viewHolder.feed)
        }

        if (viewHolder is FeedsFragmentExpandedFeedViewHolder) {
            viewHolder.updateButton.setOnClickListener {
                listener.onUpdateFeed(viewHolder.feed)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: FeedsFragmentFeedViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<FeedsFragmentViewModel.Feed>() {

        override fun areItemsTheSame(oldFeed: FeedsFragmentViewModel.Feed, newFeed: FeedsFragmentViewModel.Feed): Boolean {
            return oldFeed.id == newFeed.id
        }

        override fun areContentsTheSame(oldFeed: FeedsFragmentViewModel.Feed, newFeed: FeedsFragmentViewModel.Feed): Boolean {
            return oldFeed == newFeed
        }

    }

}
