package com.tughi.aggregator.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.tughi.aggregator.data.AppDatabase
import com.tughi.aggregator.data.Feed

class FeedListViewModel(application: Application) : AndroidViewModel(application) {

    val feeds: LiveData<List<Feed>> = AppDatabase.get(application)
            .feedDao()
            .getFeeds()

}