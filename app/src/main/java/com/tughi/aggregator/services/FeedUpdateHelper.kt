package com.tughi.aggregator.services

import android.database.Cursor
import android.util.Log
import android.util.Xml
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.App
import com.tughi.aggregator.BuildConfig
import com.tughi.aggregator.Notifications
import com.tughi.aggregator.data.Age1MonthCleanupMode
import com.tughi.aggregator.data.Age1WeekCleanupMode
import com.tughi.aggregator.data.Age1YearCleanupMode
import com.tughi.aggregator.data.Age3DaysCleanupMode
import com.tughi.aggregator.data.Age3MonthsCleanupMode
import com.tughi.aggregator.data.Age3YearsCleanupMode
import com.tughi.aggregator.data.Age6MonthsCleanupMode
import com.tughi.aggregator.data.Age6YearsCleanupMode
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.DefaultCleanupMode
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.NeverCleanupMode
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.feeds.FeedParser
import com.tughi.aggregator.preferences.UpdateSettings
import com.tughi.aggregator.utilities.Failure
import com.tughi.aggregator.utilities.Http
import com.tughi.aggregator.utilities.Success
import com.tughi.aggregator.utilities.then
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.util.Calendar
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object FeedUpdateHelper {

    val updatingFeedIds = MutableLiveData<MutableSet<Long>>()

    suspend fun updateFeed(feedId: Long) {
        val feed = Feeds.queryOne(Feeds.QueryRowCriteria(feedId), Feed.QueryHelper) ?: return
        updateFeed(feed)
    }

    suspend fun updateFeed(feed: Feed) {
        addUpdatingFeed(feed.id)
        try {
            when (val result = requestFeed(feed)) {
                is Success -> parseFeed(feed, result.data)
                is Failure -> updateFeedMetadata(feed, result.cause)
            }
        } finally {
            Notifications.refreshNewEntriesNotification(App.instance) // TODO: should be called after all feeds were updated

            withContext(NonCancellable) {
                removeUpdatingFeed(feed.id)
            }
        }
    }

    fun updateOutdatedFeeds(appLaunch: Boolean) {
        val job = GlobalScope.launch {
            val feeds = Feeds.query(Feeds.OutdatedCriteria(System.currentTimeMillis(), appLaunch), Feed.QueryHelper)

            val jobs = feeds.map { feed ->
                async {
                    updateFeed(feed)
                }
            }

            jobs.forEach {
                it.await()
            }
        }

        job.invokeOnCompletion {
            GlobalScope.launch {
                AutoUpdateScheduler.schedule()
            }
        }
    }

    private suspend fun addUpdatingFeed(feedId: Long): Unit = suspendCoroutine {
        GlobalScope.launch(Dispatchers.Main) {
            val feedIds = updatingFeedIds.value ?: mutableSetOf()
            feedIds.add(feedId)
            updatingFeedIds.value = feedIds

            it.resume(Unit)
        }
    }

    private suspend fun removeUpdatingFeed(feedId: Long): Unit = suspendCoroutine {
        GlobalScope.launch(Dispatchers.Main) {
            val feedIds = updatingFeedIds.value
            feedIds?.remove(feedId)
            updatingFeedIds.value = if (feedIds?.size != 0) feedIds else null

            it.resume(Unit)
        }
    }

    private suspend fun requestFeed(feed: Feed) = Http.request(feed.url) { requestBuilder ->
        var enableDeltaUpdates = false

        val httpEtag = feed.httpEtag
        if (httpEtag != null) {
            enableDeltaUpdates = true
            requestBuilder.addHeader("If-None-Match", httpEtag)
        }

        val httpLastModified = feed.httpLastModified
        if (httpLastModified != null) {
            enableDeltaUpdates = true
            requestBuilder.addHeader("If-Modified-Since", httpLastModified)
        }

        if (enableDeltaUpdates) {
            requestBuilder.addHeader("A-IM", "feed")
        }
    }.then { response ->
        when {
            response.isSuccessful || response.code == 304 -> Success(response)
            else -> Failure(UnexpectedHttpResponseException(response))
        }
    }

    private fun parseFeed(feed: Feed, response: Response) {
        val updateTime = System.currentTimeMillis()

        if (response.code == 304) {
            Entries.update(Entries.UpdateLastFeedUpdateEntriesCriteria(feed.id, feed.lastUpdateTime), Entries.UPDATE_TIME to updateTime)

            updateFeedMetadata(feed, updateTime)
        } else {
            val httpEtag = response.header("Etag")
            val httpLastModified = response.header("Last-Modified")

            val feedParser = FeedParser(feed.url, object : FeedParser.Listener() {
                override fun onParsedEntry(uid: String, title: String?, link: String?, content: String?, author: String?, publishDate: Date?, publishDateText: String?) {
                    val result = Entries.update(
                            Entries.UpdateFeedEntryCriteria(feed.id, uid),
                            Entries.TITLE to title,
                            Entries.LINK to link,
                            Entries.CONTENT to content,
                            Entries.AUTHOR to author,
                            Entries.PUBLISH_TIME to publishDate?.time,
                            Entries.UPDATE_TIME to updateTime
                    )

                    if (result == 0) {
                        Entries.insert(
                                Entries.FEED_ID to feed.id,
                                Entries.UID to uid,
                                Entries.TITLE to title,
                                Entries.LINK to link,
                                Entries.CONTENT to content,
                                Entries.AUTHOR to author,
                                Entries.PUBLISH_TIME to publishDate?.time,
                                Entries.INSERT_TIME to System.currentTimeMillis(),
                                Entries.UPDATE_TIME to updateTime
                        )
                    }
                }

                override fun onParsedFeed(title: String, link: String?, language: String?) {
                    updateFeedMetadata(
                            feed,
                            updateTime,
                            Feeds.TITLE to title,
                            Feeds.LINK to link,
                            Feeds.LANGUAGE to language,
                            Feeds.HTTP_ETAG to httpEtag,
                            Feeds.HTTP_LAST_MODIFIED to httpLastModified
                    )
                }
            })

            try {
                Database.transaction {
                    response.use {
                        val responseBody = response.body
                        Xml.parse(responseBody?.charStream(), feedParser.feedContentHandler)
                    }
                }
            } catch (exception: Exception) {
                updateFeedMetadata(feed, exception)
            }
        }
    }

    private fun updateFeedMetadata(feed: Feed, updateTime: Long, vararg data: Pair<Feeds.TableColumn, Any?>) {
        Database.transaction {
            val feedId = feed.id
            val nextUpdateTime = AutoUpdateScheduler.calculateNextUpdateTime(feedId, feed.updateMode, updateTime)

            Feeds.update(
                    Feeds.UpdateRowCriteria(feedId),
                    Feeds.LAST_UPDATE_TIME to updateTime,
                    Feeds.LAST_UPDATE_ERROR to null,
                    Feeds.NEXT_UPDATE_TIME to nextUpdateTime,
                    Feeds.NEXT_UPDATE_RETRY to 0,
                    *data
            )

            val deleteEntriesCriteria = createDeleteEntriesCriteria(feedId, feed.cleanupMode)
            if (deleteEntriesCriteria != null) {
                val deletedEntries = Entries.delete(deleteEntriesCriteria)
                if (BuildConfig.DEBUG) {
                    Log.d(javaClass.name, "Deleted old entries: $deletedEntries")
                }
            }
        }
    }

    private fun createDeleteEntriesCriteria(feedId: Long, cleanupMode: CleanupMode): Entries.DeleteCriteria? = when (cleanupMode) {
        DefaultCleanupMode -> createDeleteEntriesCriteria(feedId, UpdateSettings.defaultCleanupMode)
        Age3DaysCleanupMode -> Entries.DeleteOldFeedEntriesCriteria(feedId, Calendar.getInstance().apply { add(Calendar.DATE, -3) }.timeInMillis)
        Age1WeekCleanupMode -> Entries.DeleteOldFeedEntriesCriteria(feedId, Calendar.getInstance().apply { add(Calendar.DATE, -7) }.timeInMillis)
        Age1MonthCleanupMode -> Entries.DeleteOldFeedEntriesCriteria(feedId, Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis)
        Age3MonthsCleanupMode -> Entries.DeleteOldFeedEntriesCriteria(feedId, Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis)
        Age6MonthsCleanupMode -> Entries.DeleteOldFeedEntriesCriteria(feedId, Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis)
        Age1YearCleanupMode -> Entries.DeleteOldFeedEntriesCriteria(feedId, Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis)
        Age3YearsCleanupMode -> Entries.DeleteOldFeedEntriesCriteria(feedId, Calendar.getInstance().apply { add(Calendar.YEAR, -3) }.timeInMillis)
        Age6YearsCleanupMode -> Entries.DeleteOldFeedEntriesCriteria(feedId, Calendar.getInstance().apply { add(Calendar.YEAR, -6) }.timeInMillis)
        NeverCleanupMode -> null
    }

    private fun updateFeedMetadata(feed: Feed, error: Throwable?) {
        if (BuildConfig.DEBUG) {
            Log.d(javaClass.name, "Update error: $error", error)
        }

        Database.transaction {
            val updateError = when (error) {
                null -> "Unknown error"
                else -> error.message ?: error::class.java.simpleName
            }

            val nextUpdateRetry = feed.nextUpdateRetry + 1
            val nextUpdateTime = AutoUpdateScheduler.calculateNextUpdateRetryTime(feed.updateMode, nextUpdateRetry)

            Feeds.update(
                    Feeds.UpdateRowCriteria(feed.id),
                    Feeds.LAST_UPDATE_ERROR to updateError,
                    Feeds.NEXT_UPDATE_RETRY to nextUpdateRetry,
                    Feeds.NEXT_UPDATE_TIME to nextUpdateTime
            )
        }
    }

    class Feed(
            val id: Long,
            val url: String,
            val updateMode: UpdateMode,
            val cleanupMode: CleanupMode,
            val nextUpdateRetry: Int,
            val httpEtag: String?,
            val httpLastModified: String?,
            val lastUpdateTime: Long
    ) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
                Feeds.ID,
                Feeds.URL,
                Feeds.UPDATE_MODE,
                Feeds.CLEANUP_MODE,
                Feeds.NEXT_UPDATE_RETRY,
                Feeds.HTTP_ETAG,
                Feeds.HTTP_LAST_MODIFIED,
                Feeds.LAST_UPDATE_TIME
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                    id = cursor.getLong(0),
                    url = cursor.getString(1),
                    updateMode = UpdateMode.deserialize(cursor.getString(2)),
                    cleanupMode = CleanupMode.deserialize(cursor.getString(3)),
                    nextUpdateRetry = cursor.getInt(4),
                    httpEtag = cursor.getString(5),
                    httpLastModified = cursor.getString(6),
                    lastUpdateTime = cursor.getLong(7)
            )
        }
    }

    class UnexpectedHttpResponseException(response: Response) : Exception("Unexpected HTTP response: $response")

}

