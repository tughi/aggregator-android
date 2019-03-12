package com.tughi.aggregator.data

import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import java.io.Serializable

@Suppress("ClassName")
object Entries : Repository<Entries.Column, Entries.TableColumn, Entries.UpdateCriteria, Entries.DeleteCriteria, Entries.QC>("entries") {

    open class Column(override val name: String, val projection: String) : Repository.Column
    interface TableColumn : Repository.TableColumn

    object ID : Column("id", "e.id"), TableColumn
    object UID : Column("uid", "e.uid"), TableColumn
    object FEED_ID : Column("feed_id", "e.feed_id"), TableColumn
    object FEED_TITLE : Column("feed_title", "COALESCE(f.custom_title, f.title)")
    object FEED_LANGUAGE : Column("feed_language", "f.language")
    object FEED_FAVICON_URL : Column("feed_favicon_url", "f.favicon_url")
    object TITLE : Column("title", "e.title"), TableColumn
    object LINK : Column("link", "e.link"), TableColumn
    object CONTENT : Column("content", "e.content"), TableColumn
    object AUTHOR : Column("author", "e.author"), TableColumn
    object PUBLISH_TIME : Column("publish_time", "COALESCE(e.publish_time, e.insert_time)"), TableColumn
    object INSERT_TIME : Column("insert_time", "e.insert_time"), TableColumn
    object UPDATE_TIME : Column("update_time", "e.update_time"), TableColumn
    object READ_TIME : Column("read_time", "e.read_time"), TableColumn
    object PINNED_TIME : Column("pinned_time", "e.pinned_time"), TableColumn
    object TYPE : Column("type", "CASE WHEN e.read_time > 0 AND e.pinned_time = 0 THEN 'READ' ELSE 'UNREAD' END")

    fun markRead(entryId: Long): Int = update(UpdateRowCriteria(entryId), READ_TIME to System.currentTimeMillis(), PINNED_TIME to 0)

    fun markPinned(entryId: Long): Int = update(UpdateRowCriteria(entryId), READ_TIME to 0, PINNED_TIME to System.currentTimeMillis())

    fun markRead(criteria: QueryCriteria): Int {
        val selection = when (criteria) {
            is QueryCriteria.FeedEntries -> "feed_id = ? AND pinned_time = 0 AND read_time = 0"
            is QueryCriteria.MyFeedEntries -> "pinned_time = 0 AND read_time = 0"
        }
        val selectionArgs: Array<Any>? = when (criteria) {
            is QueryCriteria.FeedEntries -> arrayOf(criteria.feedId)
            is QueryCriteria.MyFeedEntries -> null
        }
        return update(SimpleUpdateCriteria(selection, selectionArgs), READ_TIME to System.currentTimeMillis())
    }

    // TODO: Use queryOne instead
    fun count(feedId: Long, since: Long): Int {
        val query = SimpleSQLiteQuery("SELECT COUNT(1) FROM entries WHERE feed_id = ? AND COALESCE(publish_time, insert_time) > ?", arrayOf(feedId, since))
        Database.query(query).use { cursor ->
            cursor.moveToNext()
            return cursor.getInt(0)
        }
    }

    // TODO: Use update with criteria instead
    fun update(feedId: Long, uid: String, vararg data: Pair<TableColumn, Any?>) = Database.update("entries", data.toContentValues(), "feed_id = ? AND uid = ?", arrayOf(feedId, uid))

    fun <T> query(criteria: QueryCriteria, factory: QueryHelper<T>): List<T> {
        val selection = when {
            criteria.sessionTime != 0L -> when (criteria) {
                is QueryCriteria.FeedEntries -> "e.feed_id = ? AND (e.read_time = 0 OR e.read_time > ?)"
                is QueryCriteria.MyFeedEntries -> "e.read_time = 0 OR e.read_time > ?"
            }
            else -> when (criteria) {
                is QueryCriteria.FeedEntries -> "e.feed_id = ?"
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
            is SortOrder.ByDateAscending -> "COALESCE(e.publish_time, e.insert_time) ASC"
            is SortOrder.ByDateDescending -> "COALESCE(e.publish_time, e.insert_time) DESC"
            is SortOrder.ByTitle -> "e.$TITLE ASC, COALESCE(e.publish_time, e.insert_time) ASC"
        }

        val query = SupportSQLiteQueryBuilder.builder("entries e LEFT JOIN feeds f ON e.feed_id = f.id")
                .columns(Array(factory.columns.size) { index -> "${factory.columns[index].projection} AS ${factory.columns[index].name}" })
                .selection(selection, selectionArgs)
                .orderBy(orderBy)
                .create()

        Database.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val entries = mutableListOf<T>()

                do {
                    entries.add(factory.createRow(cursor))
                } while (cursor.moveToNext())

                return entries
            }
        }

        return emptyList()
    }

    fun <T> liveQuery(criteria: QueryCriteria, factory: QueryHelper<T>): LiveData<List<T>> = Database.createLiveData("entries") {
        query(criteria, factory)
    }

    interface UpdateCriteria : Repository.UpdateCriteria

    private class SimpleUpdateCriteria(override val selection: String?, override val selectionArgs: Array<Any>?) : UpdateCriteria

    class UpdateRowCriteria(id: Long) : UpdateCriteria {
        override val selection = "${ID.name} = ?"
        override val selectionArgs = arrayOf<Any>(id)
    }

    interface DeleteCriteria : Repository.DeleteCriteria

    // TODO: Rename to QueryCriteria
    interface QC : Repository.QueryCriteria {

        fun config(builder: SupportSQLiteQueryBuilder)

    }

    class QueryRowCriteria(val id: Long) : QC {
        override fun config(builder: SupportSQLiteQueryBuilder) {
            builder.selection("${ID.name} = ?", arrayOf(id))
        }
    }

    // TODO: Extend from QC
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

    abstract class QueryHelper<R>(vararg columns: Column) : Repository.QueryHelper<Column, QC, R>() {

        override fun createQuery(criteria: QC): SupportSQLiteQuery = SupportSQLiteQueryBuilder
                .builder("entries e LEFT JOIN feeds f ON f.id = e.feed_id")
                .columns(Array(columns.size) { "${columns[it].projection} AS ${columns[it].name}" })
                .also { criteria.config(it) }
                .create()

    }

}
