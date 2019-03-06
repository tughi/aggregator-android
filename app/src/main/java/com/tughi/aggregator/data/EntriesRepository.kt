package com.tughi.aggregator.data

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import java.io.Serializable

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
        val selection = when {
            criteria.sessionTime != null -> when (criteria) {
                is QueryCriteria.FeedEntries -> "e.feed_id = ? AND (e.read_time = 0 OR e.read_time > ?)"
                is QueryCriteria.MyFeedEntries -> "e.read_time = 0 OR e.read_time > ?"
            }
            else -> when (criteria) {
                is QueryCriteria.FeedEntries -> "e.feed_id = ?"
                is QueryCriteria.MyFeedEntries -> null
            }
        }

        val selectionArgs = when {
            criteria.sessionTime != null -> when (criteria) {
                is QueryCriteria.FeedEntries -> arrayOf(criteria.feedId, criteria.sessionTime)
                is QueryCriteria.MyFeedEntries -> arrayOf(criteria.sessionTime)
            }
            else -> when (criteria) {
                is QueryCriteria.FeedEntries -> arrayOf(criteria.feedId)
                is QueryCriteria.MyFeedEntries -> null
            }
        }

        val orderBy = when (criteria.sortOrder) {
            is SortOrder.ByDateAscending -> "COALESCE(e.publish_time, e.insert_time) ASC"
            is SortOrder.ByDateDescending -> "COALESCE(e.publish_time, e.insert_time) DESC"
            is SortOrder.ByTitle -> "e.title ASC, COALESCE(e.publish_time, e.insert_time) ASC"
        }

        val query = SupportSQLiteQueryBuilder.builder("entries e LEFT JOIN feeds f ON e.feed_id = f.id")
                .columns(Array(columns.size) { index -> "${columns[index].projection} AS ${columns[index].column}" })
                .selection(selection, selectionArgs)
                .orderBy(orderBy)
                .create()

        Storage.query(query).use { cursor ->
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

        fun markEntriesRead(criteria: QueryCriteria): Int {
            val selection = when (criteria) {
                is QueryCriteria.FeedEntries -> "feed_id = ? AND pinned_time = 0 AND read_time = 0"
                is QueryCriteria.MyFeedEntries -> "pinned_time = 0 AND read_time = 0"
            }
            val selectionArgs: Array<Any>? = when (criteria) {
                is QueryCriteria.FeedEntries -> arrayOf(criteria.feedId)
                is QueryCriteria.MyFeedEntries -> null
            }
            return Storage.update("entries", contentValuesOf("read_time" to System.currentTimeMillis()), selection, selectionArgs)
        }

    }

    sealed class QueryCriteria : Serializable {

        abstract val sessionTime: Long?
        abstract val sortOrder: SortOrder

        data class FeedEntries(val feedId: Long, override val sessionTime: Long? = null, override val sortOrder: SortOrder) : QueryCriteria()

        data class MyFeedEntries(override val sessionTime: Long? = null, override val sortOrder: SortOrder) : QueryCriteria()

    }

    sealed class SortOrder : Serializable {

        abstract fun serialize(): String

        companion object {
            fun deserialize(value: String) = when (value) {
                "date-asc" -> ByDateAscending
                "date-desc" -> ByDateDescending
                "title-asc" -> ByTitle
                else -> ByDateAscending
            }
        }

        object ByDateAscending : SortOrder() {
            override fun serialize(): String = "date-asc"
        }

        object ByDateDescending : SortOrder() {
            override fun serialize(): String = "date-desc"
        }

        object ByTitle : SortOrder() {
            override fun serialize(): String = "title-asc"
        }

    }

}
