package com.tughi.aggregator.activities.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.data.Feed

class FeedEntriesFragmentViewModel(feedId: Long) : ViewModel() {

    val feed: LiveData<Feed> = AppDatabase.instance.feedDao().getFeed(feedId)

    class Factory(private val feedId: Long) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedEntriesFragmentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FeedEntriesFragmentViewModel(feedId) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
