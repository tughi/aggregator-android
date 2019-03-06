package com.tughi.aggregator.activities.reader

import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.tughi.aggregator.data.EntriesRepository

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

    fun getReaderActivityEntries(queryCriteria: EntriesRepository.QueryCriteria): LiveData<Array<ReaderActivityEntry>> {
        var query = """
            SELECT
                e.id,
                e.read_time,
                e.pinned_time
            FROM
                entries e
        """.trim().replace(Regex("\\s+"), " ")

        val orderBy = when (queryCriteria.sortOrder) {
            is EntriesRepository.SortOrder.ByDateAscending -> "ORDER BY COALESCE(e.publish_time, e.insert_time) ASC"
            is EntriesRepository.SortOrder.ByDateDescending -> "ORDER BY COALESCE(e.publish_time, e.insert_time) DESC"
            is EntriesRepository.SortOrder.ByTitle -> "ORDER BY e.title ASC, COALESCE(e.publish_time, e.insert_time) ASC"
        }

        val queryArgs: Array<out Any?>
        if (queryCriteria.sessionTime == null) {
            query = when (queryCriteria) {
                is EntriesRepository.QueryCriteria.FeedEntries -> "$query WHERE e.feed_id = ? $orderBy"
                is EntriesRepository.QueryCriteria.MyFeedEntries -> "$query $orderBy"
            }

            queryArgs = when (queryCriteria) {
                is EntriesRepository.QueryCriteria.FeedEntries -> arrayOf(queryCriteria.feedId)
                is EntriesRepository.QueryCriteria.MyFeedEntries -> emptyArray()
            }
        } else {
            query = when (queryCriteria) {
                is EntriesRepository.QueryCriteria.FeedEntries -> "$query WHERE e.feed_id = ? AND (e.read_time = 0 OR e.read_time > ?) $orderBy"
                is EntriesRepository.QueryCriteria.MyFeedEntries -> "$query WHERE e.read_time = 0 OR e.read_time > ? $orderBy"
            }

            queryArgs = when (queryCriteria) {
                is EntriesRepository.QueryCriteria.FeedEntries -> arrayOf(queryCriteria.feedId, queryCriteria.sessionTime)
                is EntriesRepository.QueryCriteria.MyFeedEntries -> arrayOf(queryCriteria.sessionTime)
            }
        }

        return getReaderActivityEntries(SimpleSQLiteQuery(query, queryArgs))
    }

    //    @RawQuery(observedEntities = [Entry::class, Feed::class])
    protected abstract fun getReaderActivityEntries(query: SupportSQLiteQuery): LiveData<Array<ReaderActivityEntry>>

}
