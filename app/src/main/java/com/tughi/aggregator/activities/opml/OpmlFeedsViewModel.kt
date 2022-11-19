package com.tughi.aggregator.activities.opml

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tughi.aggregator.feeds.OpmlFeed

open class OpmlFeedsViewModel : ViewModel() {
    val feeds = MutableLiveData<List<OpmlFeed>>()

    fun toggleAllFeeds() {
        val newFeeds = mutableListOf<OpmlFeed>()

        feeds.value?.forEach {
            newFeeds.add(it.copy(selected = !it.selected))
        }

        feeds.value = newFeeds
    }

    fun toggleFeed(feed: OpmlFeed) {
        val newFeeds = mutableListOf<OpmlFeed>()

        feeds.value?.forEach {
            if (it === feed) {
                newFeeds.add(it.copy(selected = !it.selected))
            } else {
                newFeeds.add(it)
            }
        }

        feeds.value = newFeeds
    }
}
