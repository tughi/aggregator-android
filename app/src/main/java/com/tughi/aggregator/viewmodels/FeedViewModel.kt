package com.tughi.aggregator.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.App
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Feed

class FeedViewModel(feedId: Long) : ViewModel() {

    val feed: LiveData<Feed> = Database.from(App.instance).feedDao().getFeed(feedId)

    class Factory(private val feedId: Long) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FeedViewModel(feedId) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
