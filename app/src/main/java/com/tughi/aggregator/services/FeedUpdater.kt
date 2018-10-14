package com.tughi.aggregator.services

import android.content.Context
import android.util.Xml
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Entry
import com.tughi.aggregator.feeds.FeedParser
import com.tughi.aggregator.utilities.Http
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

class FeedUpdater(context: Context) {

    private val database = Database.from(context)

    fun update(vararg feedIds: Long) {
        if (feedIds.isEmpty()) {
            doAsync {
                val feedIds2 = database.feedDao().queryFeedIds()
                uiThread {
                    update(*feedIds2)
                }
            }
        } else {
            for (feedId in feedIds) {
                doAsync {
                    update(feedId)
                }
            }
        }
    }

    private fun update(feedId: Long) {
        val feedDao = database.feedDao()
        val feed = feedDao.queryFeed(feedId)

        val request = Request.Builder()
                .url(feed.url)
                .build()

        val response = Http.client.newCall(request).execute()
        // TODO: handle redirected URLs
        if (response.isSuccessful) {
            val responseBody = response.body()

            if (responseBody != null) {
                val feedParser = FeedParser(feed.url, object : FeedParser.Listener() {
                    override fun onParsedFeed(
                            link: String?,
                            title: String,
                            language: String?
                    ) {
                        val now = System.currentTimeMillis()
                        val updated = feedDao.updateFeed(feed.updated(
                                url = feed.url, // TODO: save URL for permanently redirected feed
                                link = link,
                                title = title,
                                language = language,
                                updateTime = now
                        ))
                        if (updated != 1) {
                            // TODO: report that the feed couldn't be updated
                        }
                    }

                    override fun onParsedEntry(
                            uid: String,
                            link: String?,
                            title: String?,
                            content: String?,
                            author: String?,
                            publishDate: Date?,
                            publishDateText: String?
                    ) {
                        val entryDao = database.entryDao()

                        try {
                            database.beginTransaction()

                            val entry = entryDao.queryEntry(feedId, uid)
                            if (entry == null) {
                                val now = System.currentTimeMillis()
                                val entryId = entryDao.insertEntry(Entry(
                                        feedId = feedId,
                                        uid = uid,
                                        link = link,
                                        title = title,
                                        content = content,
                                        author = author,
                                        insertTime = now,
                                        publishTime = publishDate?.time ?: now,
                                        updateTime = now
                                ))
                                if (entryId == -1L) {
                                    // TODO: report that an entry couldn't be inserted
                                }
                            } else if (entry.link != link || entry.title != title || entry.content != content || entry.author != author) {
                                val now = System.currentTimeMillis()
                                val updated = entryDao.updateEntry(entry.updated(
                                        link = link,
                                        title = title,
                                        content = content,
                                        author = author,
                                        publishTime = publishDate?.time,
                                        updateTime = now
                                ))
                                if (updated != 1) {
                                    // TODO: report that an entry couldn't be updated
                                }
                            }

                            if (publishDate == null && publishDateText != null) {
                                // TODO: report that a date text couldn't be parsed
                            }

                            database.setTransactionSuccessful()
                        } finally {
                            database.endTransaction()
                        }
                    }
                })

                Xml.parse(responseBody.charStream(), feedParser.feedContentHandler)
            }
        }
    }

}
