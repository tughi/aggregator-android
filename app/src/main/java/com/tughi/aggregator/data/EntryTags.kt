package com.tughi.aggregator.data

@Suppress("ClassName")
object EntryTags : Repository<EntryTags.Column, EntryTags.TableColumn, EntryTags.UpdateCriteria, EntryTags.DeleteCriteria, EntryTags.QueryCriteria>("entry_tag") {

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("entry_tag")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object ENTRY_ID : Column("entry_id", "et.entry_id"), TableColumn
    object TAG_ID : Column("tag_id", "et.tag_id"), TableColumn
    object TAG_TIME : Column("tag_time", "et.tag_time"), TableColumn

    interface UpdateCriteria : Repository.UpdateCriteria

    interface DeleteCriteria : Repository.DeleteCriteria

    class DeleteEntryTagCriteria(entryId: Long, tagId: Long) : DeleteCriteria {
        override val selection: String = "entry_id = ? AND tag_id = ?"
        override val selectionArgs: Array<Any>? = arrayOf(entryId, tagId)
    }

    interface QueryCriteria : Repository.QueryCriteria<Column> {
        fun config(query: Query.Builder, columns: Array<out Column>)
    }

    class QueryEntryTagsCriteria(private val entryId: Long) : QueryCriteria {
        override fun config(query: Query.Builder, columns: Array<out Column>) {
            query.where("et.entry_id = ?", arrayOf(entryId))
        }
    }

    abstract class QueryHelper<Row>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, Row>(columns) {
        override fun createQuery(criteria: QueryCriteria) = Query.Builder(columns, "entry_tag et")
                .also { criteria.config(it, columns) }
                .create()
    }

}
