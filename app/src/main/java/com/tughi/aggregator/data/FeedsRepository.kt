package com.tughi.aggregator.data

import androidx.sqlite.db.SupportSQLiteQueryBuilder

class FeedsRepository<T>(private val columns: Array<Column>, private val mapper: DataMapper<T>) {

    companion object {
        val ID = Column("id", "f.id")
        val URL = Column("url", "f.url")
        val TITLE = Column("title", "COALESCE(f.custom_title, f.title)")
        val CUSTOM_TITLE = Column("custom_title", "f.custom_title")
        val LINK = Column("link", "f.link")
        val LANGUAGE = Column("language", "f.language")
        val FAVICON_URL = Column("favicon_url", "f.favicon_url")
        val FAVICON_CONTENT = Column("favicon_content", "f.favicon_content")
        val UPDATE_MODE = Column("update_mode", "f.update_mode")
        val LAST_UPDATE_TIME = Column("last_update_time", "f.last_update_time")
        val LAST_UPDATE_ERROR = Column("last_update_error", "f.last_update_error")
        val NEXT_UPDATE_TIME = Column("next_update_time", "f.next_update_time")
        val NEXT_UPDATE_RETRY = Column("next_update_retry", "f.next_update_retry")
        val HTTP_ETAG = Column("http_etag", "f.http_etag")
        val HTTP_LAST_MODIFIED = Column("http_last_modified", "f.http_last_modified")
        val UNREAD_ENTRY_COUNT = Column("unread_entry_count", "(SELECT COUNT(1) FROM entries e WHERE f.id = e.feed_id AND e.read_time = 0)")
    }

    fun insert(data: T): Long = Storage.insert("feeds", mapper.map(data))

    fun query(id: Long): T? {
        val query = SupportSQLiteQueryBuilder.builder("feeds f")
                .columns(Array(columns.size) { index -> "${columns[index].projection} AS ${columns[index].name}" })
                .selection("f.${ID.name} = ?", arrayOf(id))
                .create()

        Storage.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                return mapper.map(cursor)
            }
        }

        return null
    }

    fun query(): List<T> {
        val query = SupportSQLiteQueryBuilder.builder("feeds f")
                .columns(Array(columns.size) { index -> "${columns[index].projection} AS ${columns[index].name}" })
                .orderBy(TITLE.name) // TODO: check if title is in columns
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

    data class Column internal constructor(val name: String, val projection: String) {
        override fun toString() = name
    }

}
