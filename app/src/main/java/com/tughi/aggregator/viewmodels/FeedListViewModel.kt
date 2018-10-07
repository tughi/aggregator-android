package com.tughi.aggregator.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.UiFeed

class FeedListViewModel(application: Application) : AndroidViewModel(application) {

    val feeds: LiveData<List<UiFeed>> = Database.get(application)
            .feedDao()
            .getUiFeeds()

}
