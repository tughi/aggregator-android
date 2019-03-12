package com.tughi.aggregator.activities.main

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.Feeds

class FeedEntriesFragmentViewModel(feedId: Long) : ViewModel() {

    private val feedsFactory = object : Feeds.QueryHelper<Feed>() {
        override val columns = arrayOf<Feeds.Column>(
                Feeds.TITLE
        )

        override fun createRow(cursor: Cursor) = Feed(
                cursor.getString(0)
        )
    }

    val feed: LiveData<Feed?> = Feeds.liveQueryOne(Feeds.QueryRowCriteria(feedId), feedsFactory)

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
