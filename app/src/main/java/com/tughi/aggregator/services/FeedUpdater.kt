package com.tughi.aggregator.services

import android.database.Cursor
import android.util.Log
import android.util.Xml
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.AppDatabase
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
            mapper = object : Repository.DataMapper<Entry>() {}
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
            mapper = object : Repository.DataMapper<Feed>() {
                override fun map(cursor: Cursor) = Feed(
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

    private val database = AppDatabase.instance

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
                is Failure -> saveUpdateError(feed, result.error)
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

    private fun saveUpdateError(feed: Feed, error: Throwable?) {
        if (BuildConfig.DEBUG) {
            Log.d(javaClass.name, "Update error: $error", error)
        }

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
    }

    private fun parseFeed(feed: Feed, response: Response) {
        if (response.code() == 304) {
            updateFeed(
                    feed = feed,
                    url = feed.url,
                    title = feed.title,
                    link = feed.link,
                    language = feed.language,
                    httpEtag = feed.httpEtag,
                    httpLastModified = feed.httpLastModified
            )
        } else {
            val httpEtag = response.header("Etag")
            val httpLastModified = response.header("Last-Modified")

            val feedParser = FeedParser(feed.url, object : FeedParser.Listener() {
                override fun onParsedEntry(uid: String, title: String?, link: String?, content: String?, author: String?, publishDate: Date?, publishDateText: String?) {
                    saveEntry(
                            feedId = feed.id,
                            uid = uid,
                            title = title,
                            link = link,
                            content = content,
                            author = author,
                            publishDate = publishDate,
                            publishDateText = publishDateText
                    )
                }

                override fun onParsedFeed(title: String, link: String?, language: String?) {
                    // TODO: save URL for permanently redirected feed
                    updateFeed(
                            feed = feed,
                            url = feed.url,
                            title = title,
                            link = link,
                            language = language,
                            httpEtag = httpEtag,
                            httpLastModified = httpLastModified
                    )
                }
            })

            try {
                response.use {
                    val responseBody = response.body()
                    Xml.parse(responseBody?.charStream(), feedParser.feedContentHandler)
                }
            } catch (exception: Exception) {
                saveUpdateError(feed, exception)
            }
        }
    }

    private fun saveEntry(
            feedId: Long,
            uid: String,
            title: String?,
            link: String?,
            content: String?,
            author: String?,
            publishDate: Date?,
            publishDateText: String?
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(javaClass.name, "saveEntry($feedId, $uid, $title, $link, ...)")
        }

        try {
            database.beginTransaction()

            val entry = entries.query(feedId, uid)
            val publishTime = publishDate?.time
            if (entry == null) {
                val now = System.currentTimeMillis()
                val entryId = entries.insert(
                        Entries.FEED_ID to feedId,
                        Entries.UID to uid,
                        Entries.TITLE to title,
                        Entries.LINK to link,
                        Entries.CONTENT to content,
                        Entries.AUTHOR to author,
                        Entries.PUBLISH_TIME to publishTime,
                        Entries.INSERT_TIME to now,
                        Entries.UPDATE_TIME to now
                )
                if (entryId == -1L) {
                    // TODO: report that an entry couldn't be inserted
                }
            } else if (entry.title != title || entry.link != link || entry.content != content || entry.author != author || entry.publishTime != publishTime) {
                val now = System.currentTimeMillis()
                val updated = entries.update(
                        entry.id,
                        Entries.TITLE to title,
                        Entries.LINK to link,
                        Entries.CONTENT to content,
                        Entries.AUTHOR to author,
                        Entries.PUBLISH_TIME to publishTime,
                        Entries.UPDATE_TIME to now
                )
                if (updated != 1) {
                    // TODO: report that an entry couldn't be updated
                }
            }

            if (publishDate == null && publishDateText != null) {
                // TODO: report unsupported date format
                Log.e(javaClass.name, "Unsupported date format: $publishDateText")
            }

            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    private fun updateFeed(
            feed: Feed,
            url: String,
            title: String,
            link: String?,
            language: String?,
            httpEtag: String?,
            httpLastModified: String?
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(javaClass.name, "updateFeed($title, $link, $language, ...)")
        }

        try {
            database.beginTransaction()

            val feedId = feed.id
            val lastUpdateTime = System.currentTimeMillis()

            val nextUpdateTime = AutoUpdateScheduler.calculateNextUpdateTime(feedId, feed.updateMode, lastUpdateTime)

            val updated = database.feedDao().updateFeed(
                    id = feedId,
                    url = url,
                    title = title,
                    link = link,
                    language = language,
                    lastUpdateTime = lastUpdateTime,
                    nextUpdateTime = nextUpdateTime,
                    httpEtag = httpEtag,
                    httpLastModified = httpLastModified
            )
            if (updated != 1) {
                // TODO: report that the feed couldn't be updated
            }

            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
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

