package com.tughi.aggregator.activities.reader

import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.tughi.aggregator.data.Entries

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

    fun getReaderActivityEntries(queryCriteria: Entries.QueryCriteria): LiveData<Array<ReaderActivityEntry>> {
        var query = """
            SELECT
                e.id,
                e.read_time,
                e.pinned_time
            FROM
                entries e
        """.trim().replace(Regex("\\s+"), " ")

        val orderBy = when (queryCriteria.sortOrder) {
            is Entries.SortOrder.ByDateAscending -> "ORDER BY COALESCE(e.publish_time, e.insert_time) ASC"
            is Entries.SortOrder.ByDateDescending -> "ORDER BY COALESCE(e.publish_time, e.insert_time) DESC"
            is Entries.SortOrder.ByTitle -> "ORDER BY e.title ASC, COALESCE(e.publish_time, e.insert_time) ASC"
        }

        val queryArgs: Array<out Any?>
        if (queryCriteria.sessionTime == null) {
            query = when (queryCriteria) {
                is Entries.QueryCriteria.FeedEntries -> "$query WHERE e.feed_id = ? $orderBy"
                is Entries.QueryCriteria.MyFeedEntries -> "$query $orderBy"
            }

            queryArgs = when (queryCriteria) {
                is Entries.QueryCriteria.FeedEntries -> arrayOf(queryCriteria.feedId)
                is Entries.QueryCriteria.MyFeedEntries -> emptyArray()
            }
        } else {
            query = when (queryCriteria) {
                is Entries.QueryCriteria.FeedEntries -> "$query WHERE e.feed_id = ? AND (e.read_time = 0 OR e.read_time > ?) $orderBy"
                is Entries.QueryCriteria.MyFeedEntries -> "$query WHERE e.read_time = 0 OR e.read_time > ? $orderBy"
            }

            queryArgs = when (queryCriteria) {
                is Entries.QueryCriteria.FeedEntries -> arrayOf(queryCriteria.feedId, queryCriteria.sessionTime)
                is Entries.QueryCriteria.MyFeedEntries -> arrayOf(queryCriteria.sessionTime)
            }
        }

        return getReaderActivityEntries(SimpleSQLiteQuery(query, queryArgs))
    }

    //    @RawQuery(observedEntities = [Entry::class, Feed::class])
    protected abstract fun getReaderActivityEntries(query: SupportSQLiteQuery): LiveData<Array<ReaderActivityEntry>>

}
