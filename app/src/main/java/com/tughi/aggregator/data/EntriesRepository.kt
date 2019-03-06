package com.tughi.aggregator.data

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SupportSQLiteQueryBuilder

class EntriesRepository<T>(private val columns: Array<Column>, private val mapper: DataMapper<T>) {

    enum class Column(internal val column: String, internal val projection: String) {
        ID("id", "e.id"),
        FEED_ID("feed_id", "e.feed_id"),
        FEED_TITLE("feed_title", "COALESCE(f.custom_title, f.title)"),
        FEED_FAVICON_URL("feed_favicon_url", "f.favicon_url"),
        TITLE("title", "e.title"),
        LINK("link", "e.link"),
        AUTHOR("author", "e.author"),
        PUBLISH_TIME("publish_time", "COALESCE(e.publish_time, e.insert_time)"),
        READ_TIME("read_time", "e.read_time"),
        PINNED_TIME("pinned_time", "e.pinned_time"),
        TYPE("type", "CASE WHEN e.read_time > 0 AND e.pinned_time = 0 THEN 'READ' ELSE 'UNREAD' END")
    }

    fun query(criteria: QueryCriteria): List<T> {
        val sqliteQuery = SupportSQLiteQueryBuilder.builder("entries e LEFT JOIN feeds f ON e.feed_id = f.id")
                .columns(Array(columns.size) { index -> "${columns[index].projection} AS ${columns[index].column}" })
                .selection(criteria.selection, criteria.selectionArgs)
                .orderBy(criteria.orderBy)
                .create()

        Storage.query(sqliteQuery).use { cursor ->
            if (cursor.moveToFirst()) {
                val entries = mutableListOf<T>()

                do {
                    entries.add(mapper.map(cursor))
                } while (cursor.moveToNext())

                return entries
            }
        }

        return emptyList()
    }

    fun liveQuery(criteria: QueryCriteria): LiveData<List<T>> = Storage.createLiveData("entries") {
        query(criteria)
    }

    companion object {

        private fun update(entryId: Long, values: ContentValues): Int = Storage.update("entries", values, "id = ?", arrayOf(entryId), entryId)

        fun markEntryRead(entryId: Long): Int = update(entryId, contentValuesOf("read_time" to System.currentTimeMillis(), "pinned_time" to 0))

        fun markEntryPinned(entryId: Long): Int = update(entryId, contentValuesOf("read_time" to 0, "pinned_time" to System.currentTimeMillis()))

    }

    sealed class QueryCriteria(val sessionTime: Long?, val sortOrder: EntriesSortOrder) {

        internal abstract val selection: String?

        internal abstract val selectionArgs: Array<Any>?

        internal val orderBy: String
            get() = when (sortOrder) {
                is EntriesSortOrderByDateAsc -> "COALESCE(e.publish_time, e.insert_time) ASC"
                is EntriesSortOrderByDateDesc -> "COALESCE(e.publish_time, e.insert_time) DESC"
                is EntriesSortOrderByTitle -> "e.title ASC, COALESCE(e.publish_time, e.insert_time) ASC"
            }

        class FeedEntries(val feedId: Long, sessionTime: Long, sortOrder: EntriesSortOrder) : QueryCriteria(sessionTime, sortOrder) {

            override val selection: String?
                get() = when {
                    sessionTime != null -> "e.feed_id = ? AND (e.read_time = 0 OR e.read_time > ?)"
                    else -> "e.feed_id = ?"
                }

            override val selectionArgs: Array<Any>?
                get() = when {
                    sessionTime != null -> arrayOf(feedId, sessionTime)
                    else -> arrayOf(feedId)
                }

        }

        class MyFeedEntries(sessionTime: Long, sortOrder: EntriesSortOrder) : QueryCriteria(sessionTime, sortOrder) {

            override val selection: String?
                get() = when {
                    sessionTime != null -> "e.read_time = 0 OR e.read_time > ?"
                    else -> null
                }

            override val selectionArgs: Array<Any>?
                get() = when {
                    sessionTime != null -> arrayOf(sessionTime)
                    else -> null
                }

        }

    }

}
