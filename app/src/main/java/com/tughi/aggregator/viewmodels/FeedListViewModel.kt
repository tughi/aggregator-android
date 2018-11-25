package com.tughi.aggregator.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.data.UiFeed
import com.tughi.aggregator.services.FeedUpdater

class FeedListViewModel(application: Application) : AndroidViewModel(application) {

    private val databaseFeeds = AppDatabase.instance.feedDao().getUiFeeds()

    private val expandedFeedId = MutableLiveData<Long>()

    private val liveFeeds = MediatorLiveData<List<UiFeed>>().also {
        it.addSource(databaseFeeds) { feeds ->
            val expandedFeedId = expandedFeedId.value
            val updatingFeedIds = FeedUpdater.updatingFeedIds.value ?: emptySet<Long>()
            it.value = feeds?.map { feed ->
                val expanded = feed.id == expandedFeedId
                val updating = updatingFeedIds.contains(feed.id)
                if (expanded || updating) feed.copy(expanded = expanded, updating = updating) else feed
            } ?: emptyList()
        }
        it.addSource(expandedFeedId) { expandedFeedId ->
            val feeds = databaseFeeds.value
            val updatingFeedIds = FeedUpdater.updatingFeedIds.value ?: emptySet<Long>()
            it.value = feeds?.map { feed ->
                val expanded = feed.id == expandedFeedId
                val updating = updatingFeedIds.contains(feed.id)
                if (expanded || updating) feed.copy(expanded = expanded, updating = updating) else feed
            } ?: emptyList()
        }
        it.addSource(FeedUpdater.updatingFeedIds) { updatingFeedIds ->
            val feeds = databaseFeeds.value
            val expandedFeedId = expandedFeedId.value
            it.value = feeds?.map { feed ->
                val expanded = feed.id == expandedFeedId
                val updating = updatingFeedIds.contains(feed.id)
                if (expanded || updating) feed.copy(expanded = expanded, updating = updating) else feed
            } ?: emptyList()
        }
    }

    val feeds: LiveData<List<UiFeed>>
        get() {
            return liveFeeds
        }

    fun toggleFeed(feed: UiFeed) {
        if (expandedFeedId.value == feed.id) {
            expandedFeedId.value = null
        } else {
            expandedFeedId.value = feed.id
        }
    }

}
