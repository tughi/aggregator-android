package com.tughi.aggregator.data

import androidx.core.database.getLongOrNull
import androidx.sqlite.db.SimpleSQLiteQuery
import com.tughi.aggregator.services.AutoUpdateScheduler

@Suppress("ClassName")
object Feeds : Repository<Feeds.Column, Feeds.TableColumn, Feeds.UpdateCriteria, Feeds.DeleteCriteria, Feeds.QueryCriteria>("feed") {

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("feed")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object ID : Column("id", "f.id"), TableColumn
    object URL : Column("url", "f.url"), TableColumn
    object TITLE : Column("title", "f.title"), TableColumn
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
    object UNREAD_ENTRY_COUNT : Column("unread_entry_count", "(SELECT COUNT(1) FROM entry e WHERE f.id = e.feed_id AND e.read_time = 0)", arrayOf("feed", "entry"))

    // TODO: use DeleteCriteria
    fun delete(id: Long) = Database.delete("feed", "id = ?", arrayOf(id))

    fun queryAllCount() = Database.query(SimpleSQLiteQuery("SELECT COUNT(1) FROM feed")) { cursor ->
        cursor.moveToFirst()
        return@query cursor.getInt(0)
    }

    fun queryFirstNextUpdateTime() = Database.query(SimpleSQLiteQuery("SELECT MIN(next_update_time) FROM feed WHERE next_update_time > 0")) { cursor ->
        if (cursor.moveToFirst()) {
            return@query cursor.getLongOrNull(0)
        }
        return@query null
    }

    interface UpdateCriteria : Repository.UpdateCriteria

    class UpdateRowCriteria(id: Long) : UpdateCriteria {
        override val selection = "id = ?"
        override val selectionArgs = arrayOf<Any>(id)
    }

    interface DeleteCriteria : Repository.DeleteCriteria

    class DeleteFeedCriteria(id: Long) : DeleteCriteria {
        override val selection = "id = ?"
        override val selectionArgs = arrayOf<Any>(id)
    }

    interface QueryCriteria : Repository.QueryCriteria<Column> {
        fun config(query: Query.Builder)
    }

    class QueryRowCriteria(val id: Long) : QueryCriteria {
        override fun config(query: Query.Builder) {
            query.where("f.id = ?", arrayOf(id))
        }
    }

    class AllCriteria : QueryCriteria {
        override fun config(query: Query.Builder) {
            query.orderBy("COALESCE(f.custom_title, f.title)")
        }
    }

    class OutdatedCriteria(private val now: Long, private val appLaunch: Boolean = false) : QueryCriteria {
        override fun config(query: Query.Builder) {
            val selection = if (appLaunch) {
                "(next_update_time > 0 AND next_update_time < ?) OR next_update_time = ${AutoUpdateScheduler.NEXT_UPDATE_TIME__ON_APP_LAUNCH}"
            } else {
                "next_update_time > 0 AND next_update_time < ?"
            }
            query.where(selection, arrayOf(now))
        }
    }

    class UpdateModeCriteria(private val updateMode: UpdateMode) : QueryCriteria {
        override fun config(query: Query.Builder) {
            query.where("update_mode = ?", arrayOf(updateMode.serialize()))
        }
    }

    abstract class QueryHelper<Row>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, Row>(columns) {
        override fun createQuery(criteria: QueryCriteria) = Query.Builder(columns, "feed f")
                .also { criteria.config(it) }
                .create()
    }

}
