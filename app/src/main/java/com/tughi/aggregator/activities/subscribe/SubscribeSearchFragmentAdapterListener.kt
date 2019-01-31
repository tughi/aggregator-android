package com.tughi.aggregator.activities.subscribe

import com.tughi.aggregator.data.Feed

internal interface SubscribeSearchFragmentAdapterListener {
    fun onFeedClicked(feed: Feed)
}
