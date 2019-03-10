package com.tughi.aggregator.services

import android.database.Cursor
import android.util.Log
import android.util.Xml
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.BuildConfig
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.Repository
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.feeds.FeedParser
import com.tughi.aggregator.utilities.Failure
import com.tughi.aggregator.utilities.Http
import com.tughi.aggregator.utilities.Result
import com.tughi.aggregator.utilities.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object FeedUpdater {

    private val entries = Entries(
            columns = arrayOf(
                    Entries.ID,
                    Entries.TITLE,
                    Entries.LINK,
                    Entries.CONTENT,
                    Entries.AUTHOR,
                    Entries.PUBLISH_TIME
            ),
            factory = object : Repository.Factory<Entry>() {}
    )

    private val feeds = Feeds(
            columns = arrayOf(
                    Feeds.ID,
                    Feeds.URL,
                    Feeds.TITLE,
                    Feeds.LINK,
                    Feeds.LANGUAGE,
                    Feeds.UPDATE_MODE,
                    Feeds.NEXT_UPDATE_RETRY,
                    Feeds.HTTP_ETAG,
                    Feeds.HTTP_LAST_MODIFIED
            ),
            factory = object : Repository.Factory<Feed>() {
                override fun create(cursor: Cursor) = Feed(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        UpdateMode.deserialize(cursor.getString(5)),
                        cursor.getInt(6),
                        cursor.getString(7),
                        cursor.getString(8)
                )
            }
    )

    val updatingFeedIds = MutableLiveData<MutableSet<Long>>()

    suspend fun updateFeed(feedId: Long) {
        val feed = feeds.query(feedId) ?: return
        updateFeed(feed)
    }

    private suspend fun updateFeed(feed: Feed) {
        addUpdatingFeed(feed.id)
        try {
            val result = requestFeed(feed)
            when (result) {
                is Failure -> updateFeedContent(feed, result.error)
                is Success -> parseFeed(feed, result.data)
            }
        } finally {
            withContext(NonCancellable) {
                removeUpdatingFeed(feed.id)
            }
        }
    }

    suspend fun updateOutdatedFeeds() {
        GlobalScope.launch {
            val feeds = feeds.query(feeds.OutdatedCriteria(System.currentTimeMillis()))

            val jobs = feeds.map { feed ->
                async { FeedUpdater.updateFeed(feed) }
            }
            jobs.forEach {
                it.await()
            }
        }.also {
            it.invokeOnCompletion {
                GlobalScope.launch {
                    AutoUpdateScheduler.schedule()
                }
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

    private suspend fun requestFeed(feed: Feed): Result<Response> = suspendCancellableCoroutine {
        val request = Request.Builder()
                .url(feed.url)
                .apply {
                    var enableDeltaUpdates = false

                    val httpEtag = feed.httpEtag
                    if (httpEtag != null) {
                        enableDeltaUpdates = true
                        addHeader("If-None-Match", httpEtag)
                    }

                    val httpLastModified = feed.httpLastModified
                    if (httpLastModified != null) {
                        enableDeltaUpdates = true
                        addHeader("If-Modified-Since", httpLastModified)
                    }

                    if (enableDeltaUpdates) {
                        addHeader("A-IM", "feed")
                    }
                }
                .build()

        Http.client.newCall(request)
                .also { call ->
                    it.invokeOnCancellation {
                        call.cancel()
                    }
                }
                .enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful || response.code() == 304) {
                            it.resume(Success(response))
                        } else {
                            it.resume(Failure(UnexpectedHttpResponseException(response)))
                        }
                    }

                    override fun onFailure(call: Call, exception: IOException) {
                        it.resume(Failure(exception))
                    }
                })
    }

    private fun parseFeed(feed: Feed, response: Response) {
        if (response.code() == 304) {
            updateFeedContent(feed)
        } else {
            val httpEtag = response.header("Etag")
            val httpLastModified = response.header("Last-Modified")

            val feedParser = FeedParser(feed.url, object : FeedParser.Listener() {
                override fun onParsedEntry(uid: String, title: String?, link: String?, content: String?, author: String?, publishDate: Date?, publishDateText: String?) {
                    try {
                        entries.beginTransaction()

                        val now = System.currentTimeMillis()
                        val result = entries.update(
                                feed.id,
                                uid,
                                Entries.TITLE to title,
                                Entries.LINK to link,
                                Entries.CONTENT to content,
                                Entries.AUTHOR to author,
                                Entries.PUBLISH_TIME to publishDate?.time,
                                Entries.UPDATE_TIME to now
                        )

                        if (result == 0) {
                            entries.insert(
                                    Entries.FEED_ID to feed.id,
                                    Entries.UID to uid,
                                    Entries.TITLE to title,
                                    Entries.LINK to link,
                                    Entries.CONTENT to content,
                                    Entries.AUTHOR to author,
                                    Entries.PUBLISH_TIME to publishDate?.time,
                                    Entries.READ_TIME to 0, // TODO: add default value to the table schema
                                    Entries.PINNED_TIME to 0, // TODO: add default value to the table schema
                                    Entries.INSERT_TIME to now,
                                    Entries.UPDATE_TIME to now
                            )
                        }

                        entries.setTransactionSuccessful()
                    } finally {
                        entries.endTransaction()
                    }
                }

                override fun onParsedFeed(title: String, link: String?, language: String?) {
                    updateFeedContent(
                            feed,
                            Feeds.TITLE to title,
                            Feeds.LINK to link,
                            Feeds.LANGUAGE to language,
                            Feeds.HTTP_ETAG to httpEtag,
                            Feeds.HTTP_LAST_MODIFIED to httpLastModified
                    )
                }
            })

            try {
                response.use {
                    val responseBody = response.body()
                    Xml.parse(responseBody?.charStream(), feedParser.feedContentHandler)
                }
            } catch (exception: Exception) {
                updateFeedContent(feed, exception)
            }
        }
    }

    private fun updateFeedContent(feed: Feed, vararg data: Pair<String, Any?>) {
        try {
            feeds.beginTransaction()

            val feedId = feed.id
            val lastUpdateTime = System.currentTimeMillis()
            val nextUpdateTime = AutoUpdateScheduler.calculateNextUpdateTime(feedId, feed.updateMode, lastUpdateTime)

            feeds.update(
                    feedId,
                    Feeds.LAST_UPDATE_TIME to lastUpdateTime,
                    Feeds.LAST_UPDATE_ERROR to null,
                    Feeds.NEXT_UPDATE_TIME to nextUpdateTime,
                    Feeds.NEXT_UPDATE_RETRY to 0,
                    *data
            )

            feeds.setTransactionSuccessful()
        } finally {
            feeds.endTransaction()
        }
    }

    private fun updateFeedContent(feed: Feed, error: Throwable?) {
        if (BuildConfig.DEBUG) {
            Log.d(javaClass.name, "Update error: $error", error)
        }

        try {
            feeds.beginTransaction()

            val updateError = when (error) {
                null -> "Unknown error"
                else -> error.message ?: error::class.java.simpleName
            }

            val nextUpdateRetry = feed.nextUpdateRetry + 1
            val nextUpdateTime = AutoUpdateScheduler.calculateNextUpdateRetryTime(feed.updateMode, nextUpdateRetry)

            feeds.update(
                    feed.id,
                    Feeds.LAST_UPDATE_ERROR to updateError,
                    Feeds.NEXT_UPDATE_RETRY to nextUpdateRetry,
                    Feeds.NEXT_UPDATE_TIME to nextUpdateTime
            )

            feeds.setTransactionSuccessful()
        } finally {
            feeds.endTransaction()
        }
    }

    class Entry(
            val id: Long,
            val title: String?,
            val link: String?,
            val content: String?,
            val author: String?,
            val publishTime: Long?
    )

    class Feed(
            val id: Long,
            val url: String,
            val title: String,
            val link: String?,
            val language: String?,
            val updateMode: UpdateMode,
            val nextUpdateRetry: Int,
            val httpEtag: String?,
            val httpLastModified: String?
    )

    class UnexpectedHttpResponseException(response: Response) : Exception("Unexpected HTTP response: $response")

}

