package com.tughi.aggregator.data

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder

object Feeds : Repository() {

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

    private val projectionMap = mapOf(
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

    fun count(): Int {
        Storage.query(SimpleSQLiteQuery("SELECT COUNT(1) FROM $TABLE")).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    fun queryNextUpdateTime(): Long? {
        Storage.query(SimpleSQLiteQuery("SELECT MIN($NEXT_UPDATE_TIME) FROM $TABLE WHERE $NEXT_UPDATE_TIME > 0")).use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(0)
        }
    }

    fun queryOutdatedFeedIds(now: Long): List<Long> {
        val query = SimpleSQLiteQuery("SELECT $ID FROM $TABLE WHERE $NEXT_UPDATE_TIME > 0 AND $NEXT_UPDATE_TIME < ?", arrayOf(now))
        Storage.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val result = mutableListOf<Long>()
                do {
                    result.add(cursor.getLong(0))
                } while (cursor.moveToNext())
                return result
            }
        }
        return emptyList()
    }

    fun insert(vararg data: Pair<String, Any?>): Long = Storage.insert(TABLE, data.toContentValues())

    fun update(id: Long, vararg data: Pair<String, Any?>) = Storage.update(TABLE, data.toContentValues(), "$ID = ?", arrayOf(id), id)

    override fun createQueryOneSelection() = "f.$ID = ?"

    override fun createQueryBuilder(columns: Array<String>): SupportSQLiteQueryBuilder = SupportSQLiteQueryBuilder.builder("$TABLE f").also {
        it.columns(Array(columns.size) { index -> "${projectionMap[columns[index]]} AS ${columns[index]}" })
    }

    fun <T> liveQuery(id: Long, factory: Factory<T>) = Storage.createLiveData(TABLE) { query(id, factory) }

    fun <T> liveQuery(criteria: Criteria, factory: Factory<T>) = Storage.createLiveData(TABLE) { query(criteria, factory) }

    class AllCriteria : Criteria {
        override fun init(builder: SupportSQLiteQueryBuilder) {
            /* TODO:
            if (factory.columns.contains(TITLE)) {
                builder.orderBy(TITLE)
            }
            */
        }
    }

    class OutdatedCriteria(private val now: Long) : Criteria {
        override fun init(builder: SupportSQLiteQueryBuilder) {
            builder.selection("($NEXT_UPDATE_TIME > 0 AND $NEXT_UPDATE_TIME < ?) OR $NEXT_UPDATE_TIME = -1", arrayOf(now))
        }
    }

    class UpdateModeCriteria(private val updateMode: UpdateMode) : Criteria {
        override fun init(builder: SupportSQLiteQueryBuilder) {
            builder.selection("$UPDATE_MODE = ?", arrayOf(updateMode.serialize()))
        }
    }

}
