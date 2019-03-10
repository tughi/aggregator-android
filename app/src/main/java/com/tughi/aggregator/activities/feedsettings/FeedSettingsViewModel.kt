package com.tughi.aggregator.activities.feedsettings

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.Repository
import com.tughi.aggregator.data.UpdateMode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FeedSettingsViewModel(feedId: Long) : ViewModel() {

    val repository = Feeds(
            object : Repository.Factory<Feed>() {
                override val columns = arrayOf(
                        Feeds.ID,
                        Feeds.URL,
                        Feeds.TITLE,
                        Feeds.CUSTOM_TITLE,
                        Feeds.UPDATE_MODE
                )

                override fun create(cursor: Cursor) = Feed(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        UpdateMode.deserialize(cursor.getString(4))
                )
            }
    )

    val feed: LiveData<Feed>

    var newUpdateMode: UpdateMode? = null

    init {
        val liveFeed = MutableLiveData<Feed>()

        GlobalScope.launch {
            liveFeed.postValue(repository.query(feedId))
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
