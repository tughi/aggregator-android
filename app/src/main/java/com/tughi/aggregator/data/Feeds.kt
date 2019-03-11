package com.tughi.aggregator.data

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder

@Suppress("ClassName")
object Feeds : Repository<Feeds.TableColumn, Feeds.Column>() {

    open class Column(override val name: String, val projection: String) : Repository.Column
    interface TableColumn : Repository.TableColumn

    object ID : Column("id", "f.id"), TableColumn
    object URL : Column("url", "f.url"), TableColumn
    object TITLE : Column("title", "COALESCE(f.custom_title, f.title)"), TableColumn
    object CUSTOM_TITLE : Column("custom_title", "f.custom_title"), TableColumn
    object LINK : Column("link", "f.link"), TableColumn
    object LANGUAGE : Column("language", "f.language"), TableColumn
    object FAVICON_URL : Column("favicon_url", "f.favicon_url"), TableColumn
    object FAVICON_CONTENT : Column("favicon_content", "f.favicon_content"), TableColumn
    object UPDATE_MODE : Column("update_mode", "f.update_mode"), TableColumn
    object LAST_UPDATE_TIME : Column("last_update_time", "f.last_update_time"), TableColumn
    object LAST_UPDATE_ERROR : Column("last_update_error", "f.last_update_error"), TableColumn
    object NEXT_UPDATE_TIME : Column("next_update_time", "f.next_update_time"), TableColumn
    object NEXT_UPDATE_RETRY : Column("next_update_retry", "f.next_update_retry"), TableColumn
    object HTTP_ETAG : Column("http_etag", "f.http_etag"), TableColumn
    object HTTP_LAST_MODIFIED : Column("http_last_modified", "f.http_last_modified"), TableColumn
    object UNREAD_ENTRY_COUNT : Column("unread_entry_count", "(SELECT COUNT(1) FROM entries e WHERE f.id = e.feed_id AND e.read_time = 0)")

    override val tableName = "feeds"

    fun delete(id: Long) = Storage.delete("feeds", "id = ?", arrayOf(id))

    fun count(): Int {
        Storage.query(SimpleSQLiteQuery("SELECT COUNT(1) FROM feeds")).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    fun queryNextUpdateTime(): Long? {
        Storage.query(SimpleSQLiteQuery("SELECT MIN(next_update_time) FROM feeds WHERE next_update_time > 0")).use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(0)
        }
    }

    fun queryOutdatedFeedIds(now: Long): List<Long> {
        val query = SimpleSQLiteQuery("SELECT id FROM feeds WHERE next_update_time > 0 AND next_update_time < ?", arrayOf(now))
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

    override fun createQueryOneSelection() = "f.id = ?"

    override fun createQueryBuilder(columns: Array<Column>): SupportSQLiteQueryBuilder = SupportSQLiteQueryBuilder.builder("feeds f").also {
        it.columns(Array(columns.size) { index -> "${columns[index].projection} AS ${columns[index].name}" })
    }

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
            builder.selection("(next_update_time > 0 AND next_update_time < ?) OR next_update_time = -1", arrayOf(now))
        }
    }

    class UpdateModeCriteria(private val updateMode: UpdateMode) : Criteria {
        override fun init(builder: SupportSQLiteQueryBuilder) {
            builder.selection("update_mode = ?", arrayOf(updateMode.serialize()))
        }
    }

    abstract class Factory<R> : Repository.Factory<Column, R>()

}
