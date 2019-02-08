package com.tughi.aggregator.activities.main

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.tughi.aggregator.data.EntriesQuery
import com.tughi.aggregator.data.FeedEntriesQuery
import com.tughi.aggregator.data.MyFeedEntriesQuery

@Dao
abstract class MainDao {

    @Query("""
        SELECT
            f.id,
            COALESCE(f.custom_title, f.title) AS title,
            f.favicon_url,
            f.last_update_time,
            f.last_update_error,
            f.next_update_time,
            f.next_update_retry,
            f.update_mode,
            (SELECT COUNT(1) FROM entries e WHERE f.id = e.feed_id AND e.read_time = 0) AS unread_entry_count,
            0 AS expanded,
            0 AS updating
        FROM
            feeds f
        ORDER BY
            title
    """)
    abstract fun getFeedsFragmentFeeds(): LiveData<List<FeedsFragmentFeed>>

    fun getEntriesFragmentEntries(query: EntriesQuery): LiveData<Array<EntriesFragmentEntry>> = when (query) {
        is FeedEntriesQuery -> getEntriesFragmentFeedEntries(query.feedId, query.since)
        is MyFeedEntriesQuery -> getEntriesFragmentMyFeedEntries(query.since)
    }

    @Query("""
        SELECT
            e.id,
            f.id AS feed_id,
            COALESCE(f.custom_title, f.title) AS feed_title,
            f.favicon_url,
            e.title,
            e.link,
            e.author,
            COALESCE(e.publish_time, e.insert_time) AS formatted_date,
            COALESCE(e.publish_time, e.insert_time) AS formatted_time,
            e.read_time,
            e.pinned_time,
            (e.read_time > 0 AND e.pinned_time = 0) AS type
        FROM
            entries e
            LEFT JOIN feeds f ON f.id = e.feed_id
        WHERE
            e.feed_id = :feedId AND
            (e.read_time = 0 OR e.read_time > :since OR e.pinned_time > 0)
        ORDER BY
            COALESCE(e.publish_time, e.insert_time)
    """)
    abstract fun getEntriesFragmentFeedEntries(feedId: Long, since: Long): LiveData<Array<EntriesFragmentEntry>>

    @Query("""
        SELECT
            e.id,
            f.id AS feed_id,
            COALESCE(f.custom_title, f.title) AS feed_title,
            f.favicon_url,
            e.title,
            e.link,
            e.author,
            COALESCE(e.publish_time, e.insert_time) AS formatted_date,
            COALESCE(e.publish_time, e.insert_time) AS formatted_time,
            e.read_time,
            e.pinned_time,
            (e.read_time > 0 AND e.pinned_time = 0) AS type
        FROM
            entries e
            LEFT JOIN feeds f ON f.id = e.feed_id
        WHERE
            (e.read_time = 0 OR e.read_time > :since OR e.pinned_time > 0)
        ORDER BY
            COALESCE(e.publish_time, e.insert_time)
    """)
    abstract fun getEntriesFragmentMyFeedEntries(since: Long): LiveData<Array<EntriesFragmentEntry>>

}
