package com.tughi.aggregator.data

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import java.io.Serializable

@Suppress("ClassName")
object Entries : Repository<Entries.Column, Entries.TableColumn, Entries.UpdateCriteria, Entries.DeleteCriteria, Entries.QueryCriteria>("entries") {

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("entries")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object ID : Column("id", "e.id"), TableColumn
    object UID : Column("uid", "e.uid"), TableColumn
    object FEED_ID : Column("feed_id", "e.feed_id"), TableColumn
    object FEED_TITLE : Column("feed_title", "COALESCE(f.custom_title, f.title)", arrayOf("feeds"))
    object FEED_LANGUAGE : Column("feed_language", "f.language", arrayOf("feeds"))
    object FEED_FAVICON_URL : Column("feed_favicon_url", "f.favicon_url", arrayOf("feeds"))
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

    fun markRead(entryId: Long): Int = update(UpdateEntryCriteria(entryId), READ_TIME to System.currentTimeMillis(), PINNED_TIME to 0)

    fun markPinned(entryId: Long): Int = update(UpdateEntryCriteria(entryId), READ_TIME to 0, PINNED_TIME to System.currentTimeMillis())

    fun markRead(criteria: EntriesQueryCriteria): Int {
        val selection = when (criteria) {
            is FeedEntriesQueryCriteria -> "feed_id = ? AND pinned_time = 0 AND read_time = 0"
            is MyFeedEntriesQueryCriteria -> "pinned_time = 0 AND read_time = 0"
            else -> throw IllegalArgumentException("Unsupported criteria: $criteria")
        }
        val selectionArgs: Array<Any>? = when (criteria) {
            is FeedEntriesQueryCriteria -> arrayOf(criteria.feedId)
            is MyFeedEntriesQueryCriteria -> null
            else -> throw IllegalArgumentException("Unsupported criteria: $criteria")
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

    interface UpdateCriteria : Repository.UpdateCriteria

    private class SimpleUpdateCriteria(override val selection: String?, override val selectionArgs: Array<Any>?) : UpdateCriteria {
        override val affectedRowId: Any? = null
    }

    class UpdateFeedEntryCriteria(feedId: Long, uid: String) : UpdateCriteria {
        override val affectedRowId: Any? = null
        override val selection = "feed_id = ? AND uid = ?"
        override val selectionArgs = arrayOf(feedId, uid)
    }

    class UpdateEntryCriteria(id: Long) : UpdateCriteria {
        override val affectedRowId: Any? = id
        override val selection = "id = ?"
        override val selectionArgs = arrayOf<Any>(id)
    }

    interface DeleteCriteria : Repository.DeleteCriteria

    interface QueryCriteria : Repository.QueryCriteria<Column> {

        fun config(builder: SupportSQLiteQueryBuilder)

    }

    class QueryRowCriteria(val id: Long) : QueryCriteria {

        override fun config(builder: SupportSQLiteQueryBuilder) {
            builder.selection("e.id = ?", arrayOf(id))
        }

    }

    abstract class EntriesQueryCriteria : QueryCriteria, Serializable {

        abstract val sessionTime: Long
        abstract val sortOrder: SortOrder

        abstract fun copy(sessionTime: Long? = null, sortOrder: SortOrder? = null): EntriesQueryCriteria

    }

    class FeedEntriesQueryCriteria(val feedId: Long, override val sessionTime: Long = 0, override val sortOrder: SortOrder) : EntriesQueryCriteria() {

        override fun config(builder: SupportSQLiteQueryBuilder) {
            val selection: String?
            val selectionArgs: Array<Long>?
            if (sessionTime != 0L) {
                selection = "e.feed_id = ? AND (e.read_time = 0 OR e.read_time > ?)"
                selectionArgs = arrayOf(feedId, sessionTime)
            } else {
                selection = "e.feed_id = ?"
                selectionArgs = arrayOf(feedId)
            }
            builder.selection(selection, selectionArgs)
            builder.orderBy(sortOrder.orderBy)
        }

        override fun copy(sessionTime: Long?, sortOrder: SortOrder?) = FeedEntriesQueryCriteria(
                feedId = feedId,
                sessionTime = sessionTime ?: this.sessionTime,
                sortOrder = sortOrder ?: this.sortOrder
        )

    }

    class MyFeedEntriesQueryCriteria(override val sessionTime: Long = 0, override val sortOrder: SortOrder) : EntriesQueryCriteria() {

        override fun config(builder: SupportSQLiteQueryBuilder) {
            val selection: String?
            val selectionArgs: Array<Long>?
            if (sessionTime != 0L) {
                selection = "e.read_time = 0 OR e.read_time > ?"
                selectionArgs = arrayOf(sessionTime)
            } else {
                selection = null
                selectionArgs = null
            }
            builder.selection(selection, selectionArgs)
            builder.orderBy(sortOrder.orderBy)
        }

        override fun copy(sessionTime: Long?, sortOrder: SortOrder?) = Entries.MyFeedEntriesQueryCriteria(
                sessionTime = sessionTime ?: this.sessionTime,
                sortOrder = sortOrder ?: this.sortOrder
        )

    }

    sealed class SortOrder(private val value: String, internal val orderBy: String) : Serializable {

        fun serialize() = value

        companion object {
            fun deserialize(value: String) = when (value) {
                "date-asc" -> ByDateAscending
                "date-desc" -> ByDateDescending
                "title-asc" -> ByTitle
                else -> ByDateAscending
            }
        }

        object ByDateAscending : SortOrder("date-asc", "COALESCE(e.publish_time, e.insert_time) ASC")

        object ByDateDescending : SortOrder("date-desc", "COALESCE(e.publish_time, e.insert_time) DESC")

        object ByTitle : SortOrder("title-asc", "e.title ASC, COALESCE(e.publish_time, e.insert_time) ASC")

    }

    abstract class QueryHelper<R>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, R>(columns) {

        override fun createQuery(criteria: QueryCriteria): SupportSQLiteQuery = SupportSQLiteQueryBuilder
                .builder("entries e LEFT JOIN feeds f ON f.id = e.feed_id")
                .columns(Array(columns.size) { "${columns[it].projection} AS ${columns[it].name}" })
                .also { criteria.config(it) }
                .create()

    }

}
