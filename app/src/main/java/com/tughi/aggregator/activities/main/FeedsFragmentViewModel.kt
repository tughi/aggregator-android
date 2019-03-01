package com.tughi.aggregator.activities.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.services.FeedUpdater

class FeedsFragmentViewModel(application: Application) : AndroidViewModel(application) {

    private val databaseFeeds = AppDatabase.instance.mainDao().getFeedsFragmentFeeds()

    private val expandedFeedId = MutableLiveData<Long>()

    val feeds: LiveData<List<FeedsFragmentFeed>> = MediatorLiveData<List<FeedsFragmentFeed>>().also {
        it.addSource(databaseFeeds) { feeds ->
            it.value = transformFeeds(feeds, expandedFeedId.value, FeedUpdater.updatingFeedIds.value.orEmpty())
        }
        it.addSource(expandedFeedId) { expandedFeedId ->
            it.value = transformFeeds(databaseFeeds.value, expandedFeedId, FeedUpdater.updatingFeedIds.value.orEmpty())
        }
        it.addSource(FeedUpdater.updatingFeedIds) { updatingFeedIds ->
            it.value = transformFeeds(databaseFeeds.value, expandedFeedId.value, updatingFeedIds.orEmpty())
        }
    }

    private fun transformFeeds(feeds: List<FeedsFragmentFeed>?, expandedFeedId: Long?, updatingFeedIds: Set<Long>) = feeds?.map { feed ->
        val expanded = feed.id == expandedFeedId
        val updating = updatingFeedIds.contains(feed.id)
        if (expanded || updating) feed.copy(expanded = expanded, updating = updating) else feed
    }

    fun toggleFeed(feed: FeedsFragmentFeed) {
        if (expandedFeedId.value == feed.id) {
            expandedFeedId.value = null
        } else {
            expandedFeedId.value = feed.id
        }
    }

}
