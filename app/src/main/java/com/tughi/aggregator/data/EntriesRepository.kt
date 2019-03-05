package com.tughi.aggregator.data

import android.content.ContentValues
import android.database.Cursor
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SupportSQLiteQueryBuilder

class EntriesRepository<T>(private val columns: Array<Column>, private val mapper: Mapper<T>) {

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
        TYPE("type", "CASE WHEN e.read_time > 0 AND e.pinned_time = 0 THEN 'UNREAD' ELSE 'READ' END")
    }

    fun query(criteria: EntriesQuery): List<T> {
        val sqliteQuery = SupportSQLiteQueryBuilder.builder("entries e LEFT JOIN feeds f ON e.feed_id = f.id")
                .columns(Array(columns.size) { index -> "${columns[index].projection} AS ${columns[index].column}" })
                .create()

        Storage.readableDatabase.query(sqliteQuery).use { cursor ->
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

    fun liveQuery(criteria: EntriesQuery): LiveData<List<T>> = Storage.createLiveData("entries") {
        query(criteria)
    }

    companion object {

        private fun update(entryId: Long, values: ContentValues): Int {
            val result = Storage.writableDatabase.update("entries", 0, values, "id = ?", arrayOf(entryId))
            if (result > 0) {
                Storage.invalidateLiveData("entries", entryId)
            }
            return result
        }

        fun markEntryRead(entryId: Long): Int = update(entryId, contentValuesOf("read_time" to System.currentTimeMillis(), "pinned_time" to 0))

        fun markEntryPinned(entryId: Long): Int = update(entryId, contentValuesOf("read_time" to 0, "pinned_time" to System.currentTimeMillis()))

    }

    interface Mapper<T> {

        fun map(cursor: Cursor): T

    }

}
