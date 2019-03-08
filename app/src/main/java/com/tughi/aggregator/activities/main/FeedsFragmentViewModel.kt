package com.tughi.aggregator.activities.main

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.data.DataMapper
import com.tughi.aggregator.data.FeedsRepository
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.services.FeedUpdater
import java.io.Serializable

class FeedsFragmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FeedsRepository(
            arrayOf(
                    FeedsRepository.ID,
                    FeedsRepository.TITLE,
                    FeedsRepository.FAVICON_URL,
                    FeedsRepository.LAST_UPDATE_TIME,
                    FeedsRepository.LAST_UPDATE_ERROR,
                    FeedsRepository.NEXT_UPDATE_TIME,
                    FeedsRepository.NEXT_UPDATE_RETRY,
                    FeedsRepository.UPDATE_MODE,
                    FeedsRepository.UNREAD_ENTRY_COUNT
            ),
            object : DataMapper<Feed>() {
                override fun map(cursor: Cursor) = Feed(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        faviconUrl = cursor.getString(2),
                        lastUpdateTime = cursor.getLong(3),
                        lastUpdateError = cursor.getString(4),
                        nextUpdateTime = cursor.getLong(5),
                        nextUpdateRetry = cursor.getInt(6),
                        updateMode = UpdateMode.deserialize(cursor.getString(7)),
                        unreadEntryCount = cursor.getInt(8)
                )
            }
    )

    private val databaseFeeds = repository.liveQuery()

    private val expandedFeedId = MutableLiveData<Long>()

    val feeds: LiveData<List<Feed>> = MediatorLiveData<List<Feed>>().also {
        it.addSource(databaseFeeds) { feeds ->
            it.value = transformFeeds(feeds, expandedFeedId.value, FeedUpdater.updatingFeedIds.value.orEmpty())
        }
        it.addSource(expandedFeedId) { expandedFeedId ->
            it.value = transformFeeds(databaseFeeds.value, expandedFeedId, FeedUpdater.updatingFeedIds.value.orEmpty())
        }
        it.addSource(FeedUpdater.updatingFeedIds) { updatingFeedIds ->
            it.value = transformFeeds(databaseFeeds.value, expandedFeedId.value, updatingFeedIds.orEmpty())
        }
    }

    private fun transformFeeds(feeds: List<Feed>?, expandedFeedId: Long?, updatingFeedIds: Set<Long>) = feeds?.map { feed ->
        val expanded = feed.id == expandedFeedId
        val updating = updatingFeedIds.contains(feed.id)
        if (expanded || updating) feed.copy(expanded = expanded, updating = updating) else feed
    }

    fun toggleFeed(feed: Feed) {
        if (expandedFeedId.value == feed.id) {
            expandedFeedId.value = null
        } else {
            expandedFeedId.value = feed.id
        }
    }

    data class Feed(
            val id: Long,
            val title: String,
            val faviconUrl: String?,
            val lastUpdateTime: Long,
            val lastUpdateError: String?,
            val nextUpdateTime: Long,
            val nextUpdateRetry: Int,
            val updateMode: UpdateMode,
            val unreadEntryCount: Int,
            val expanded: Boolean = false,
            val updating: Boolean = false
    ) : Serializable

}
