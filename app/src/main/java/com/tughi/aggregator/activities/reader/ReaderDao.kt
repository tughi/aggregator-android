package com.tughi.aggregator.activities.reader

import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.tughi.aggregator.data.EntriesQuery
import com.tughi.aggregator.data.EntriesSortOrderByDateAsc
import com.tughi.aggregator.data.EntriesSortOrderByDateDesc
import com.tughi.aggregator.data.EntriesSortOrderByTitle
import com.tughi.aggregator.data.FeedEntriesQuery
import com.tughi.aggregator.data.MyFeedEntriesQuery

//@Dao
abstract class ReaderDao {

//    @Query("""
//        SELECT
//            e.id,
//            e.title,
//            e.link,
//            e.content,
//            e.author,
//            COALESCE(e.publish_time, e.insert_time) AS publish_time,
//            COALESCE(f.custom_title, f.title) AS feed_title,
//            f.language AS feed_language,
//            e.read_time,
//            e.pinned_time
//        FROM
//            entries e
//            LEFT JOIN feeds f ON f.id = e.feed_id
//        WHERE e.id = :entryId
//    """)
    abstract fun getReaderFragmentEntry(entryId: Long): LiveData<ReaderFragmentEntry>

    fun getReaderActivityEntries(entriesQuery: EntriesQuery): LiveData<Array<ReaderActivityEntry>> {
        var query = """
            SELECT
                e.id,
                e.read_time,
                e.pinned_time
            FROM
                entries e
        """.trim().replace(Regex("\\s+"), " ")

        val orderBy = when (entriesQuery.sortOrder) {
            is EntriesSortOrderByDateAsc -> "ORDER BY COALESCE(e.publish_time, e.insert_time) ASC"
            is EntriesSortOrderByDateDesc -> "ORDER BY COALESCE(e.publish_time, e.insert_time) DESC"
            is EntriesSortOrderByTitle -> "ORDER BY e.title ASC, COALESCE(e.publish_time, e.insert_time) ASC"
        }

        val queryArgs: Array<out Any>
        if (entriesQuery.sessionTime == 0L) {
            query = when (entriesQuery) {
                is FeedEntriesQuery -> "$query WHERE e.feed_id = ? $orderBy"
                is MyFeedEntriesQuery -> "$query $orderBy"
            }

            queryArgs = when (entriesQuery) {
                is FeedEntriesQuery -> arrayOf(entriesQuery.feedId)
                is MyFeedEntriesQuery -> emptyArray()
            }
        } else {
            query = when (entriesQuery) {
                is FeedEntriesQuery -> "$query WHERE e.feed_id = ? AND (e.read_time = 0 OR e.read_time > ?) $orderBy"
                is MyFeedEntriesQuery -> "$query WHERE e.read_time = 0 OR e.read_time > ? $orderBy"
            }

            queryArgs = when (entriesQuery) {
                is FeedEntriesQuery -> arrayOf(entriesQuery.feedId, entriesQuery.sessionTime)
                is MyFeedEntriesQuery -> arrayOf(entriesQuery.sessionTime)
            }
        }

        return getReaderActivityEntries(SimpleSQLiteQuery(query, queryArgs))
    }

//    @RawQuery(observedEntities = [Entry::class, Feed::class])
    protected abstract fun getReaderActivityEntries(query: SupportSQLiteQuery): LiveData<Array<ReaderActivityEntry>>

}
