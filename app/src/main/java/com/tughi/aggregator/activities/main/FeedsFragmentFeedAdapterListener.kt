package com.tughi.aggregator.activities.main

internal interface FeedsFragmentFeedAdapterListener {

    fun onFeedClicked(feed: FeedsFragmentViewModel.Feed)

    fun onToggleFeed(feed: FeedsFragmentViewModel.Feed)

    fun onUpdateFeed(feed: FeedsFragmentViewModel.Feed)

}
