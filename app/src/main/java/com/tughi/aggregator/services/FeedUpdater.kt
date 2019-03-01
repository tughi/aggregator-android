package com.tughi.aggregator.services

import android.util.Log
import android.util.Xml
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.BuildConfig
import com.tughi.aggregator.data.Entry
import com.tughi.aggregator.data.Feed
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

    private val database = AppDatabase.instance

    val updatingFeedIds = MutableLiveData<MutableSet<Long>>()

    suspend fun updateFeed(feedId: Long) {
        addUpdatingFeed(feedId)

        try {
            val feed = database.feedDao().queryFeed(feedId)

            val result = requestFeed(feed)
            when (result) {
                is Failure -> saveUpdateError(feed, result.error)
                is Success -> parseFeed(feed, result.data)
            }

        } finally {
            withContext(NonCancellable) {
                removeUpdatingFeed(feedId)
            }
        }
    }

    suspend fun updateOutdatedFeeds() {
        GlobalScope.launch(Dispatchers.IO) {
            val feeds = database.feedDao().queryOutdatedFeeds(System.currentTimeMillis())

            val jobs = feeds.map { feedId ->
                async { FeedUpdater.updateFeed(feedId) }
            }
            jobs.forEach {
                it.await()
            }
        }.also {
            it.invokeOnCompletion {
                GlobalScope.launch(Dispatchers.IO) {
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
            Log.d(javaClass.name, "Update error: $error")
        }

        val nextUpdateRetry = feed.nextUpdateRetry + 1

        database.feedDao().updateFeed(
                id = feed.id!!,
                lastUpdateError = when (error) {
                    null -> "Unknown error"
                    else -> error.message ?: error::class.java.simpleName
                },
                nextUpdateRetry = nextUpdateRetry,
                nextUpdateTime = AutoUpdateScheduler.calculateNextUpdateRetryTime(feed.updateMode, nextUpdateRetry)
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
                            feedId = feed.id!!,
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

        val entryDao = database.entryDao()

        try {
            database.beginTransaction()

            val entry = entryDao.queryEntry(feedId, uid)
            val publishTime = publishDate?.time
            if (entry == null) {
                val now = System.currentTimeMillis()
                val entryId = entryDao.insertEntry(Entry(
                        feedId = feedId,
                        uid = uid,
                        title = title,
                        link = link,
                        content = content,
                        author = author,
                        insertTime = now,
                        publishTime = publishTime,
                        updateTime = now
                ))
                if (entryId == -1L) {
                    // TODO: report that an entry couldn't be inserted
                }
            } else if (entry.title != title || entry.link != link || entry.content != content || entry.author != author || entry.publishTime != publishTime) {
                val now = System.currentTimeMillis()
                val updated = entryDao.updateEntry(
                        id = entry.id!!,
                        title = title,
                        link = link,
                        content = content,
                        author = author,
                        publishTime = publishTime,
                        updateTime = now
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

            val feedId = feed.id!!
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

    class UnexpectedHttpResponseException(response: Response) : Exception("Unexpected HTTP response: $response")

}

