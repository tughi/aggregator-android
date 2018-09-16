package com.tughi.aggregator.viewmodels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import com.tughi.aggregator.data.AppDatabase
import com.tughi.aggregator.data.Feed

class FeedListViewModel(application: Application) : AndroidViewModel(application) {

    val feeds: LiveData<List<Feed>> = AppDatabase.getInstance(application)
            .feedDao()
            .getFeeds()

}