package com.tughi.aggregator.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.App
import com.tughi.aggregator.data.Database

class FeedSettingsViewModel(feedId: Long) : ViewModel() {

    val feed = Database.from(App.instance).feedDao()
            .getFeed(feedId)

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
