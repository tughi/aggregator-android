package com.tughi.aggregator.services

import android.util.Log
import android.util.Xml
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.data.Entry
import com.tughi.aggregator.feeds.FeedParser
import com.tughi.aggregator.utilities.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Request
import java.util.*

object FeedUpdater {

    private val database = AppDatabase.instance

    fun update(vararg feedIds: Long) {
        if (feedIds.isEmpty()) {
            GlobalScope.launch {
                val feedIds2 = database.feedDao().queryFeedIds()
                launch(Dispatchers.Main) {
                    update(*feedIds2)
                }
            }
        } else {
            for (feedId in feedIds) {
                GlobalScope.launch {
                    update(feedId)
                }
            }
        }
    }

    private fun update(feedId: Long) {
        val feed = database.feedDao().queryFeed(feedId)

        val request = Request.Builder()
                .url(feed.url)
                .build()

        val response = Http.client.newCall(request).execute()
        // TODO: handle redirected URLs
        if (response.isSuccessful) {
            val responseBody = response.body()

            if (responseBody != null) {
                val feedParser = FeedParser(feed.url, object : FeedParser.Listener() {
                    override fun onParsedFeed(title: String, link: String?, language: String?) {
                        // TODO: save URL for permanently redirected feed
                        updateFeed(id = feedId, url = feed.url, title = title, link = link, language = language)
                    }

                    override fun onParsedEntry(uid: String, title: String?, link: String?, content: String?, author: String?, publishDate: Date?, publishDateText: String?) {
                        saveEntry(feedId = feedId, uid = uid, title = title, link = link, content = content, author = author, publishDate = publishDate, publishDateText = publishDateText)
                    }
                })

                Xml.parse(responseBody.charStream(), feedParser.feedContentHandler)
            }
        }
    }

    fun saveEntry(
            feedId: Long,
            uid: String,
            title: String?,
            link: String?,
            content: String?,
            author: String?,
            publishDate: Date?,
            publishDateText: String?
    ) {
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

    fun updateFeed(
            id: Long,
            url: String,
            title: String,
            link: String?,
            language: String?
    ) {

        val now = System.currentTimeMillis()
        val updated = database.feedDao().updateFeed(
                id = id,
                url = url,
                title = title,
                link = link,
                language = language,
                lastUpdateTime = now
        )
        if (updated != 1) {
            // TODO: report that the feed couldn't be updated
        }
    }

}
