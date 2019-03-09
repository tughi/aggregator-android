package com.tughi.aggregator.activities.main

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.Repository

class FeedEntriesFragmentViewModel(feedId: Long) : ViewModel() {

    private val feeds = Feeds(
            columns = arrayOf(
                    Feeds.TITLE
            ),
            mapper = object : Repository.DataMapper<Feed>() {
                override fun map(cursor: Cursor) = Feed(
                        cursor.getString(0)
                )
            }
    )

    val feed: LiveData<Feed?> = feeds.liveQuery(feedId)

    class Feed(val title: String)

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
