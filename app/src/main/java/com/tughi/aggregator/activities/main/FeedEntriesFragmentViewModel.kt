package com.tughi.aggregator.activities.main

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.Feeds

class FeedEntriesFragmentViewModel(feedId: Long) : ViewModel() {

    val feed: LiveData<Feed?> = Feeds.liveQueryOne(Feeds.QueryRowCriteria(feedId), Feed.QueryHelper)

    class Feed(val title: String) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
                Feeds.TITLE,
                Feeds.CUSTOM_TITLE
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                    title = cursor.getString(1) ?: cursor.getString(0)
            )
        }
    }

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
