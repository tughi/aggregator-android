package com.tughi.aggregator.activities.subscribe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.tughi.aggregator.R

internal class SubscribeSearchFragmentAdapter(private val listener: SubscribeSearchFragmentAdapterListener) : ListAdapter<Any, SubscribeSearchFragmentViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SubscribeSearchFragmentViewModel.Feed -> R.layout.subscribe_feed_item
            is Boolean -> R.layout.subscribe_loading_item
            is String -> R.layout.subscribe_message_item
            else -> throw IllegalStateException("Unsupported item")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscribeSearchFragmentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.subscribe_feed_item -> SubscribeSearchFragmentFeedViewHolder(itemView, listener)
            R.layout.subscribe_loading_item -> SubscribeSearchFragmentLoadingViewHolder(itemView)
            R.layout.subscribe_message_item -> SubscribeSearchFragmentMessageViewHolder(itemView)
            else -> throw IllegalStateException("Unsupported item view type")
        }
    }

    override fun onBindViewHolder(holder: SubscribeSearchFragmentViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when (oldItem) {
                is SubscribeSearchFragmentViewModel.Feed -> newItem is SubscribeSearchFragmentViewModel.Feed && oldItem.url == newItem.url
                is Boolean -> newItem is Boolean
                is String -> newItem is String
                else -> throw IllegalStateException("Unsupported old item")
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when (oldItem) {
                is SubscribeSearchFragmentViewModel.Feed -> newItem is SubscribeSearchFragmentViewModel.Feed && oldItem.url == newItem.url && oldItem.title == newItem.title
                is Boolean -> newItem is Boolean && newItem == oldItem
                is String -> newItem is String && newItem == oldItem
                else -> throw IllegalStateException("Unsupported old item")
            }
        }
    }

}
