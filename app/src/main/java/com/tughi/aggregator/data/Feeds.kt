package com.tughi.aggregator.data

import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder

class Feeds<T>(columns: Array<String> = emptyArray(), mapper: Repository.DataMapper<T>) : Repository<T>(columns, mapper) {

    companion object {
        internal const val TABLE = "feeds"

        const val ID = "id"
        const val URL = "url"
        const val TITLE = "title"
        const val CUSTOM_TITLE = "custom_title"
        const val LINK = "link"
        const val LANGUAGE = "language"
        const val FAVICON_URL = "favicon_url"
        const val FAVICON_CONTENT = "favicon_content"
        const val UPDATE_MODE = "update_mode"
        const val LAST_UPDATE_TIME = "last_update_time"
        const val LAST_UPDATE_ERROR = "last_update_error"
        const val NEXT_UPDATE_TIME = "next_update_time"
        const val NEXT_UPDATE_RETRY = "next_update_retry"
        const val HTTP_ETAG = "http_etag"
        const val HTTP_LAST_MODIFIED = "http_last_modified"
        const val UNREAD_ENTRY_COUNT = "unread_entry_count"

        internal val projectionMap = mapOf(
                ID to "f.$ID",
                URL to "f.$URL",
                TITLE to "COALESCE(f.$CUSTOM_TITLE, f.$TITLE)",
                CUSTOM_TITLE to "f.$CUSTOM_TITLE",
                LINK to "f.$LINK",
                LANGUAGE to "f.$LANGUAGE",
                FAVICON_URL to "f.$FAVICON_URL",
                FAVICON_CONTENT to "f.$FAVICON_CONTENT",
                UPDATE_MODE to "f.$UPDATE_MODE",
                LAST_UPDATE_TIME to "f.$LAST_UPDATE_TIME",
                LAST_UPDATE_ERROR to "f.$LAST_UPDATE_ERROR",
                NEXT_UPDATE_TIME to "f.$NEXT_UPDATE_TIME",
                NEXT_UPDATE_RETRY to "f.$NEXT_UPDATE_RETRY",
                HTTP_ETAG to "f.$HTTP_ETAG",
                HTTP_LAST_MODIFIED to "f.$HTTP_LAST_MODIFIED",
                UNREAD_ENTRY_COUNT to "(SELECT COUNT(1) FROM ${Entries.TABLE} e WHERE f.$ID = e.${Entries.FEED_ID} AND e.${Entries.READ_TIME} = 0)"
        )

        fun delete(id: Long) = Storage.delete(TABLE, "$ID = ?", arrayOf(id))

    }

    fun insert(vararg data: Pair<String, Any?>): Long = Storage.insert(TABLE, mapper.map(data))

    fun update(id: Long, vararg data: Pair<String, Any?>) = Storage.update(TABLE, mapper.map(data), "$ID = ?", arrayOf(id), id)

    fun query(id: Long): T? {
        val query = SupportSQLiteQueryBuilder.builder("$TABLE f")
                .columns(Array(columns.size) { index -> "${projectionMap[columns[index]]} AS ${columns[index]}" })
                .selection("f.$ID = ?", arrayOf(id))
                .create()

        Storage.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                return mapper.map(cursor)
            }
        }

        return null
    }

    fun liveQuery(id: Long) = Storage.createLiveData(TABLE) { query(id) }

    fun query(criteria: Criteria): List<T> {
        Storage.query(criteria.query).use { cursor ->
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

    fun liveQuery(criteria: Criteria) = Storage.createLiveData(TABLE) { query(criteria) }

    abstract inner class Criteria {

        val query: SupportSQLiteQuery = SupportSQLiteQueryBuilder.builder("$TABLE f").also {
            it.columns(Array(columns.size) { index -> "${projectionMap[columns[index]]} AS ${columns[index]}" })
            init(it)
        }.create()

        abstract fun init(builder: SupportSQLiteQueryBuilder)

    }

    inner class AllCriteria : Criteria() {
        override fun init(builder: SupportSQLiteQueryBuilder) {
            if (columns.contains(TITLE)) {
                builder.orderBy(TITLE)
            }
        }
    }

    inner class OutdatedCriteria(private val now: Long) : Criteria() {
        override fun init(builder: SupportSQLiteQueryBuilder) {
            builder.selection("($NEXT_UPDATE_TIME > 0 AND $NEXT_UPDATE_TIME < ?) OR $NEXT_UPDATE_TIME = -1", arrayOf(now))
        }
    }

}
