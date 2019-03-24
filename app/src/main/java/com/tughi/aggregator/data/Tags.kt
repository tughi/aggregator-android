package com.tughi.aggregator.data

import androidx.core.content.contentValuesOf
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder

@Suppress("ClassName")
object Tags : Repository<Tags.Column, Tags.TableColumn, Tags.UpdateCriteria, Tags.DeleteCriteria, Tags.QueryCriteria>("tag") {

    const val STARRED = 0L
    const val HIDDEN = -1L

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("tag")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object ID : Column("id", "t.id"), TableColumn
    object NAME : Column("name", "t.name"), TableColumn
    object EDITABLE : Column("editable", "t.editable"), TableColumn
    object ENTRY_COUNT : Column("entry_count", "SUM(CASE WHEN et.tag_time THEN 1 ELSE 0 END)", arrayOf("entry_tag", "tag"))
    object UNREAD_ENTRY_COUNT : Column("unread_entry_count", "SUM(CASE WHEN e.read_time = 0 THEN 1 WHEN e.pinned_time THEN 1 ELSE 0 END)", arrayOf("entry", "entry_tag", "tag"))

    fun addTag(entryId: Long, tagId: Long) = Database.replace("entry_tag", contentValuesOf("entry_id" to entryId, "tag_id" to tagId, "tag_time" to System.currentTimeMillis()))

    fun removeTag(entryId: Long, tagId: Long) = Database.replace("entry_tag", contentValuesOf("entry_id" to entryId, "tag_id" to tagId, "tag_time" to 0))

    interface UpdateCriteria : Repository.UpdateCriteria

    class UpdateTagCriteria(tagId: Long) : UpdateCriteria {
        override val affectedRowId: Any? = tagId
        override val selection = "id = ?"
        override val selectionArgs = arrayOf<Any>(tagId)
    }

    interface DeleteCriteria : Repository.DeleteCriteria

    class DeleteTagCriteria(tagId: Long) : DeleteCriteria {
        override val selection = "id = ? AND editable = 1"
        override val selectionArgs = arrayOf<Any>(tagId)
    }

    interface QueryCriteria : Repository.QueryCriteria<Column> {
        fun config(builder: SupportSQLiteQueryBuilder, columns: Array<out Column>)
    }

    class QueryTagCriteria(val tagId: Long) : QueryCriteria {
        override fun config(builder: SupportSQLiteQueryBuilder, columns: Array<out Column>) {
            builder.selection("t.id = ?", arrayOf(tagId))
        }
    }

    object QueryVisibleTagsCriteria : QueryCriteria {
        override fun config(builder: SupportSQLiteQueryBuilder, columns: Array<out Column>) {
            builder.selection("t.id != ?", arrayOf(HIDDEN))
        }
    }

    abstract class QueryHelper<Row>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, Row>(columns) {

        private val tables: String

        init {
            val tables = StringBuilder("tag t")

            var entry = false
            var entryTag = false
            for (column in columns) {
                if (column !is TableColumn) {
                    when (column) {
                        is ENTRY_COUNT -> {
                            entryTag = true
                        }
                        is UNREAD_ENTRY_COUNT -> {
                            entry = true
                            entryTag = true
                        }
                    }
                }
                if (entry && entryTag) {
                    break
                }
            }
            if (entryTag) {
                tables.append(" LEFT JOIN entry_tag et ON et.tag_id = t.id")
            }
            if (entry) {
                tables.append(" LEFT JOIN entry e ON e.id = et.entry_id")
            }

            this.tables = tables.toString()
        }


        override fun createQuery(criteria: QueryCriteria): SupportSQLiteQuery = SupportSQLiteQueryBuilder
                .builder(tables)
                .columns(Array(columns.size) { "${columns[it].projection} AS ${columns[it].name}" })
                .groupBy("t.id")
                .orderBy("t.name")
                .also { criteria.config(it, columns) }
                .create()

    }

}
