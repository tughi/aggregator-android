package com.tughi.aggregator.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.App
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Feed

class FeedSettingsViewModel(feedId: Long) : ViewModel() {

    private val liveFeed = MediatorLiveData<Feed>()

    val feed: LiveData<Feed>
        get() = liveFeed

    init {
        val databaseFeed = Database.from(App.instance).feedDao().getFeed(feedId)

        liveFeed.addSource(databaseFeed) { newFeed ->
            if (liveFeed.value == null) {
                liveFeed.value = newFeed

                liveFeed.removeSource(databaseFeed)
            }
        }
    }

    class Factory(private val feedId: Long) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedSettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FeedSettingsViewModel(feedId) as T
            }
            throw UnsupportedOperationException()
        }

    }


}
