package com.tughi.aggregator.data

import androidx.core.content.contentValuesOf
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder

@Suppress("ClassName")
object Tags : Repository<Tags.Column, Tags.TableColumn, Tags.UpdateCriteria, Tags.DeleteCriteria, Tags.QueryCriteria>("tag") {

    const val STAR = 0L
    const val HIDE = -1L

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("tag")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object ID : Column("id", "t.id"), TableColumn
    object NAME : Column("name", "t.name"), TableColumn
    object EDITABLE : Column("editable", "t.editable"), TableColumn

    fun addTag(entryId: Long, tagId: Long) = Database.replace("entry_tag", contentValuesOf("entry_id" to entryId, "tag_id" to tagId, "tag_time" to System.currentTimeMillis()))

    fun removeTag(entryId: Long, tagId: Long) = Database.replace("entry_tag", contentValuesOf("entry_id" to entryId, "tag_id" to tagId, "tag_time" to 0))

    interface UpdateCriteria : Repository.UpdateCriteria

    interface DeleteCriteria : Repository.DeleteCriteria

    interface QueryCriteria : Repository.QueryCriteria<Column> {
        fun config(builder: SupportSQLiteQueryBuilder, columns: Array<out Column>)
    }

    abstract class QueryHelper<Row>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, Row>(columns) {

        override fun createQuery(criteria: QueryCriteria): SupportSQLiteQuery = SupportSQLiteQueryBuilder
                .builder("tag t")
                .columns(Array(columns.size) { "${columns[it].projection} AS ${columns[it].name}" })
                .also { criteria.config(it, columns) }
                .create()

    }

}
