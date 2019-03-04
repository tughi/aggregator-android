package com.tughi.aggregator.data

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EntriesRepository<T>(private val columns: Array<Column>, private val mapper: Mapper<T>) {

    enum class Column(internal val projection: String) {
        ID("e.id"),
        FEED_ID("e.feed_id"),
        FEED_TITLE("COALESCE(f.custom_title, f.title)"),
        FEED_FAVICON_URL("f.favicon_url"),
        TITLE("e.title"),
        LINK("e.link"),
        AUTHOR("e.author"),
        FORMATTED_DATE("COALESCE(e.publish_time, e.insert_time)"),
        FORMATTED_TIME("COALESCE(e.publish_time, e.insert_time)"),
        READ_TIME("e.read_time"),
        PINNED_TIME("e.pinned_time"),
        TYPE("CASE WHEN e.read_time > 0 AND e.pinned_time = 0 THEN 'UNREAD' ELSE 'READ' END"),
    }

    fun query(criteria: EntriesQuery): List<T> {
        val sqliteQuery = SupportSQLiteQueryBuilder.builder("entries e LEFT JOIN feeds f ON e.feed_id = f.id")
                .columns(Array(columns.size) { index -> "${columns[index].projection} AS ${columns[index].name.toLowerCase()}" })
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

    fun liveQuery(criteria: EntriesQuery): LiveData<List<T>> {
        val liveData = MutableLiveData<List<T>>()

        GlobalScope.launch {
            liveData.postValue(query(criteria))
        }

        return liveData
    }

    interface Mapper<T> {

        fun map(cursor: Cursor): T

    }

}
