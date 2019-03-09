package com.tughi.aggregator.data

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import java.io.Serializable

class Entries<T>(columns: Array<String>, mapper: DataMapper<T>) : Repository<T>(columns, mapper) {

    companion object {
        internal const val TABLE = "entries"

        const val ID = "id"
        const val UID = "uid"
        const val FEED_ID = "feed_id"
        const val FEED_TITLE = "feed_title"
        const val FEED_FAVICON_URL = "feed_favicon_url"
        const val TITLE = "title"
        const val LINK = "link"
        const val CONTENT = "content"
        const val AUTHOR = "author"
        const val PUBLISH_TIME = "publish_time"
        const val INSERT_TIME = "insert_time"
        const val UPDATE_TIME = "update_time"
        const val READ_TIME = "read_time"
        const val PINNED_TIME = "pinned_time"

        const val TYPE = "type"

        internal val projectionMap = mapOf(
                ID to "e.$ID",
                UID to "e.$UID",
                FEED_ID to "e.$FEED_ID",
                FEED_TITLE to "COALESCE(f.${Feeds.CUSTOM_TITLE}, f.${Feeds.TITLE})",
                FEED_FAVICON_URL to "f.${Feeds.FAVICON_URL}",
                TITLE to "e.$TITLE",
                LINK to "e.$LINK",
                AUTHOR to "e.$AUTHOR",
                PUBLISH_TIME to "COALESCE(e.$PUBLISH_TIME, e.$INSERT_TIME)",
                READ_TIME to "e.$READ_TIME",
                PINNED_TIME to "e.$PINNED_TIME",
                TYPE to "CASE WHEN e.$READ_TIME > 0 AND e.$PINNED_TIME = 0 THEN 'READ' ELSE 'UNREAD' END"
        )

        private fun update(entryId: Long, values: ContentValues): Int = Storage.update(TABLE, values, "$ID = ?", arrayOf(entryId), entryId)

        fun markRead(entryId: Long): Int = update(entryId, contentValuesOf(READ_TIME to System.currentTimeMillis(), PINNED_TIME to 0))

        fun markPinned(entryId: Long): Int = update(entryId, contentValuesOf(READ_TIME to 0, PINNED_TIME to System.currentTimeMillis()))

        fun markRead(criteria: QueryCriteria): Int {
            val selection = when (criteria) {
                is QueryCriteria.FeedEntries -> "$FEED_ID = ? AND $PINNED_TIME = 0 AND $READ_TIME = 0"
                is QueryCriteria.MyFeedEntries -> "$PINNED_TIME = 0 AND $READ_TIME = 0"
            }
            val selectionArgs: Array<Any>? = when (criteria) {
                is QueryCriteria.FeedEntries -> arrayOf(criteria.feedId)
                is QueryCriteria.MyFeedEntries -> null
            }
            return Storage.update(TABLE, contentValuesOf(READ_TIME to System.currentTimeMillis()), selection, selectionArgs)
        }

        fun count(feedId: Long, since: Long): Int {
            val query = SimpleSQLiteQuery("SELECT COUNT(1) FROM $TABLE WHERE $FEED_ID = ? AND COALESCE($PUBLISH_TIME, $INSERT_TIME) > ?", arrayOf(feedId, since))
            Storage.query(query).use { cursor ->
                cursor.moveToNext()
                return cursor.getInt(0)
            }
        }

    }

    fun insert(vararg data: Pair<String, Any?>): Long = Storage.insert(TABLE, mapper.map(data))

    fun update(id: Long, vararg data: Pair<String, Any?>) = Storage.update(TABLE, mapper.map(data), "$ID = ?", arrayOf(id), id)

    fun update(feedId: Long, uid: String, vararg data: Pair<String, Any?>) = Storage.update(TABLE, mapper.map(data), "$FEED_ID = ? AND $UID = ?", arrayOf(feedId, uid))

    fun query(feedId: Long, uid: String): T? {
        val query = SupportSQLiteQueryBuilder.builder("$TABLE e")
                .columns(Array(columns.size) { index -> "${projectionMap[columns[index]]} AS ${columns[index]}" })
                .selection("e.$FEED_ID = ? AND e.$UID = ?", arrayOf(feedId, uid))
                .create()

        Storage.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                return mapper.map(cursor)
            }
        }

        return null
    }

    fun query(criteria: QueryCriteria): List<T> {
        val selection = when {
            criteria.sessionTime != 0L -> when (criteria) {
                is QueryCriteria.FeedEntries -> "e.$FEED_ID = ? AND (e.$READ_TIME = 0 OR e.$READ_TIME > ?)"
                is QueryCriteria.MyFeedEntries -> "e.$READ_TIME = 0 OR e.$READ_TIME > ?"
            }
            else -> when (criteria) {
                is QueryCriteria.FeedEntries -> "e.$FEED_ID = ?"
                is QueryCriteria.MyFeedEntries -> null
            }
        }

        val selectionArgs = when {
            criteria.sessionTime != 0L -> when (criteria) {
                is QueryCriteria.FeedEntries -> arrayOf(criteria.feedId, criteria.sessionTime)
                is QueryCriteria.MyFeedEntries -> arrayOf(criteria.sessionTime)
            }
            else -> when (criteria) {
                is QueryCriteria.FeedEntries -> arrayOf(criteria.feedId)
                is QueryCriteria.MyFeedEntries -> null
            }
        }

        val orderBy = when (criteria.sortOrder) {
            is SortOrder.ByDateAscending -> "COALESCE(e.$PUBLISH_TIME, e.$INSERT_TIME) ASC"
            is SortOrder.ByDateDescending -> "COALESCE(e.$PUBLISH_TIME, e.$INSERT_TIME) DESC"
            is SortOrder.ByTitle -> "e.$TITLE ASC, COALESCE(e.$PUBLISH_TIME, e.$INSERT_TIME) ASC"
        }

        val query = SupportSQLiteQueryBuilder.builder("$TABLE e LEFT JOIN ${Feeds.TABLE} f ON e.$FEED_ID = f.${Feeds.ID}")
                .columns(Array(columns.size) { index -> "${projectionMap[columns[index]]} AS ${columns[index]}" })
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

    fun liveQuery(criteria: QueryCriteria): LiveData<List<T>> = Storage.createLiveData(TABLE) {
        query(criteria)
    }

    sealed class QueryCriteria : Serializable {

        abstract val sessionTime: Long
        abstract val sortOrder: SortOrder

        data class FeedEntries(val feedId: Long, override val sessionTime: Long = 0, override val sortOrder: SortOrder) : QueryCriteria()

        data class MyFeedEntries(override val sessionTime: Long = 0, override val sortOrder: SortOrder) : QueryCriteria()

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
