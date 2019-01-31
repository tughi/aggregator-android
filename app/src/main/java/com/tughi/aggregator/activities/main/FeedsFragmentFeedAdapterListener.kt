package com.tughi.aggregator.activities.main

internal interface FeedsFragmentFeedAdapterListener {

    fun onFeedClicked(feed: FeedsFragmentFeed)

    fun onToggleFeed(feed: FeedsFragmentFeed)

    fun onUpdateFeed(feed: FeedsFragmentFeed)

}
