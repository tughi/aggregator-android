package com.tughi.aggregator.activities.reader

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.tughi.aggregator.data.EntriesQuery
import com.tughi.aggregator.data.EntriesSortOrder
import com.tughi.aggregator.data.EntriesSortOrderByDateAsc
import com.tughi.aggregator.data.EntriesSortOrderByDateDesc
import com.tughi.aggregator.data.EntriesSortOrderByTitle
import com.tughi.aggregator.data.Entry
import com.tughi.aggregator.data.Feed
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

    fun getReaderActivityEntries(entriesQuery: EntriesQuery, entriesSortOrder: EntriesSortOrder = EntriesSortOrderByDateAsc): LiveData<Array<ReaderActivityEntry>> {
        var query = """
            SELECT
                e.id,
                e.read_time,
                e.pinned_time
            FROM
                entries e
        """.trim().replace(Regex("\\s+"), " ")

        val orderBy = when (entriesSortOrder) {
            EntriesSortOrderByDateAsc -> "ORDER BY COALESCE(e.publish_time, e.insert_time) ASC"
            EntriesSortOrderByDateDesc -> "ORDER BY COALESCE(e.publish_time, e.insert_time) DESC"
            EntriesSortOrderByTitle -> "ORDER BY e.title ASC, COALESCE(e.publish_time, e.insert_time) ASC"
        }

        query = when (entriesQuery) {
            is FeedEntriesQuery -> "$query WHERE e.feed_id = ? AND (e.read_time = 0 OR e.read_time > ?) $orderBy"
            is MyFeedEntriesQuery -> "$query WHERE (e.read_time = 0 OR e.read_time > ?) $orderBy"
        }

        val queryArgs = when (entriesQuery) {
            is FeedEntriesQuery -> arrayOf(entriesQuery.feedId, entriesQuery.since)
            is MyFeedEntriesQuery -> arrayOf(entriesQuery.since)
        }

        return getReaderActivityEntries(SimpleSQLiteQuery(query, queryArgs))
    }

    @RawQuery(observedEntities = [Entry::class, Feed::class])
    protected abstract fun getReaderActivityEntries(query: SupportSQLiteQuery): LiveData<Array<ReaderActivityEntry>>

}
