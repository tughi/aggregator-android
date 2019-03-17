package com.tughi.aggregator.activities.subscribe

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R

internal open class SubscribeSearchFragmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    open fun onBind(item: Any) {}

}

internal class SubscribeSearchFragmentFeedViewHolder(itemView: View, listener: SubscribeSearchFragmentAdapterListener) : SubscribeSearchFragmentViewHolder(itemView) {

    private val titleTextView = itemView.findViewById<TextView>(R.id.title)
    private val urlTextView = itemView.findViewById<TextView>(R.id.url)

    private lateinit var feed: SubscribeSearchFragmentViewModel.Feed

    init {
        itemView.setOnClickListener {
            listener.onFeedClicked(feed)
        }
    }

    override fun onBind(item: Any) {
        feed = item as SubscribeSearchFragmentViewModel.Feed
        titleTextView.text = feed.title
        urlTextView.text = feed.url
    }

}

internal class SubscribeSearchFragmentLoadingViewHolder(itemView: View) : SubscribeSearchFragmentViewHolder(itemView)

internal class SubscribeSearchFragmentMessageViewHolder(itemView: View) : SubscribeSearchFragmentViewHolder(itemView) {

    private val textView = itemView as TextView

    override fun onBind(item: Any) {
        textView.text = item as String
    }

}
