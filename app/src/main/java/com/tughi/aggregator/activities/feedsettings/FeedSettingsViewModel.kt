package com.tughi.aggregator.activities.feedsettings

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.UpdateMode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FeedSettingsViewModel(feedId: Long) : ViewModel() {

    private val feedsQueryHelper = object : Feeds.QueryHelper<Feed>() {
        override val columns = arrayOf<Feeds.Column>(
                Feeds.ID,
                Feeds.URL,
                Feeds.TITLE,
                Feeds.CUSTOM_TITLE,
                Feeds.UPDATE_MODE
        )

        override fun createRow(cursor: Cursor) = Feed(
                id = cursor.getLong(0),
                url = cursor.getString(1),
                title = cursor.getString(2),
                customTitle = cursor.getString(3),
                updateMode = UpdateMode.deserialize(cursor.getString(4))
        )
    }

    val feed: LiveData<Feed>

    var newUpdateMode: UpdateMode? = null

    init {
        val liveFeed = MutableLiveData<Feed>()

        GlobalScope.launch {
            liveFeed.postValue(Feeds.queryOne(Feeds.QueryRowCriteria(feedId), feedsQueryHelper))
        }

        feed = liveFeed
    }

    class Feed(
            val id: Long,
            val url: String,
            val title: String,
            val customTitle: String?,
            val updateMode: UpdateMode
    )

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
