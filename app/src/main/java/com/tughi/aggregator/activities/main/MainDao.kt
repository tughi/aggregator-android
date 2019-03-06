package com.tughi.aggregator.activities.main

import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.tughi.aggregator.data.EntriesQuery
import com.tughi.aggregator.data.FeedEntriesQuery
import com.tughi.aggregator.data.MyFeedEntriesQuery

abstract class MainDao {

//    @Query("""
//        SELECT
//            f.id,
//            COALESCE(f.custom_title, f.title) AS title,
//            f.favicon_url,
//            f.last_update_time,
//            f.last_update_error,
//            f.next_update_time,
//            f.next_update_retry,
//            f.update_mode,
//            (SELECT COUNT(1) FROM entries e WHERE f.id = e.feed_id AND e.read_time = 0) AS unread_entry_count,
//            0 AS expanded,
//            0 AS updating
//        FROM
//            feeds f
//        ORDER BY
//            title
//    """)
    abstract fun getFeedsFragmentFeeds(): LiveData<List<FeedsFragmentFeed>>

    fun markAllEntriesRead(entriesQuery: EntriesQuery) {
        var query = "UPDATE entries SET read_time = ?"

        query = when (entriesQuery) {
            is FeedEntriesQuery -> "$query WHERE feed_id = ? AND pinned_time = 0 AND read_time = 0"
            is MyFeedEntriesQuery -> "$query WHERE pinned_time = 0 AND read_time = 0"
        }

        val readTime = System.currentTimeMillis()

        val queryArgs = when (entriesQuery) {
            is FeedEntriesQuery -> arrayOf(readTime, entriesQuery.feedId)
            is MyFeedEntriesQuery -> arrayOf(readTime)
        }

        markAllEntriesRead(SimpleSQLiteQuery(query, queryArgs))

//        AppDatabase.instance.invalidationTracker.refreshVersionsAsync()
    }

//    @RawQuery
    protected abstract fun markAllEntriesRead(query: SupportSQLiteQuery): Int

}
