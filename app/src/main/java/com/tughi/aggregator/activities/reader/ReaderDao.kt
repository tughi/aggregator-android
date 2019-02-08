package com.tughi.aggregator.activities.reader

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.tughi.aggregator.data.EntriesQuery
import com.tughi.aggregator.data.FeedEntriesQuery
import com.tughi.aggregator.data.MyFeedEntriesQuery

@Dao
abstract class ReaderDao {

    @Query("""
        SELECT
            e.id,
            e.title,
            e.link,
            e.content,
            e.author,
            COALESCE(e.publish_time, e.insert_time) AS publish_time,
            COALESCE(f.custom_title, f.title) AS feed_title,
            f.language AS feed_language,
            e.read_time,
            e.pinned_time
        FROM
            entries e
            LEFT JOIN feeds f ON f.id = e.feed_id
        WHERE e.id = :entryId
    """)
    abstract fun getReaderFragmentEntry(entryId: Long): LiveData<ReaderFragmentEntry>

    fun getReaderActivityEntries(query: EntriesQuery): LiveData<Array<ReaderActivityEntry>> = when (query) {
        is FeedEntriesQuery -> getFeedReaderActivityEntries(query.feedId, query.since)
        is MyFeedEntriesQuery -> getMyFeedReaderActivityEntries(query.since)
    }

    @Query("""
        SELECT
            e.id,
            e.read_time,
            e.pinned_time
        FROM
            entries e
        WHERE
            e.feed_id = :feedId AND
            (e.read_time = 0 OR e.read_time > :since)
        ORDER BY
            COALESCE(e.publish_time, e.insert_time)
    """)
    abstract fun getFeedReaderActivityEntries(feedId: Long, since: Long): LiveData<Array<ReaderActivityEntry>>

    @Query("""
        SELECT
            e.id,
            e.read_time,
            e.pinned_time
        FROM
            entries e
        WHERE
            (e.read_time = 0 OR e.read_time > :since)
        ORDER BY
            COALESCE(e.publish_time, e.insert_time)
    """)
    abstract fun getMyFeedReaderActivityEntries(since: Long): LiveData<Array<ReaderActivityEntry>>

}
