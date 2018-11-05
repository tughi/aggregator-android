package com.tughi.aggregator.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.data.UiFeed

class FeedListViewModel(application: Application) : AndroidViewModel(application) {

    private val databaseFeeds = AppDatabase.instance.feedDao().getUiFeeds()

    private val expandedFeedId = MutableLiveData<Long>()

    private val liveFeeds = MediatorLiveData<List<UiFeed>>().also {
        it.addSource(databaseFeeds) { feeds ->
            val expandedFeedId = expandedFeedId.value
            it.value = feeds?.map { feed ->
                if (feed.id == expandedFeedId) feed.copy(expanded = true) else feed
            }
        }
        it.addSource(expandedFeedId) { expandedFeedId ->
            val feeds = databaseFeeds.value
            it.value = feeds?.map { feed ->
                if (feed.id == expandedFeedId) feed.copy(expanded = true) else feed
            }
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
