package com.tughi.aggregator.data

import androidx.sqlite.db.SupportSQLiteQueryBuilder

class FeedsRepository<T>(private val columns: Array<Column>, private val mapper: DataMapper<T>) {

    enum class Column(internal val column: String, internal val projection: String) {
        ID("id", "f.id"),
        TITLE("title", "COALESCE(f.custom_title, f.title)"),
        FAVICON_URL("favicon_url", "f.favicon_url"),
        LAST_UPDATE_TIME("last_update_time", "f.last_update_time"),
        LAST_UPDATE_ERROR("last_update_error", "f.last_update_error"),
        NEXT_UPDATE_TIME("next_update_time", "f.next_update_time"),
        NEXT_UPDATE_RETRY("next_update_retry", "f.next_update_retry"),
        UPDATE_MODE("update_mode", "f.update_mode"),
        UNREAD_ENTRY_COUNT("unread_entry_count", "(SELECT COUNT(1) FROM entries e WHERE f.id = e.feed_id AND e.read_time = 0)"),
    }

    fun query(): List<T> {
        val query = SupportSQLiteQueryBuilder.builder("feeds f")
                .columns(Array(columns.size) { index -> "${columns[index].projection} AS ${columns[index].column}" })
                .orderBy(Column.TITLE.column) // TODO: check if title is in columns
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

    fun liveQuery() = Storage.createLiveData("feeds") { query() }

    companion object {

    }

}
