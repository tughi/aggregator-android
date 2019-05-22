package com.tughi.aggregator.data

@Suppress("ClassName")
object Tags : Repository<Tags.Column, Tags.TableColumn, Tags.UpdateCriteria, Tags.DeleteCriteria, Tags.QueryCriteria>("tag") {

    const val STARRED = 0L
    const val HIDDEN = -1L

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("tag")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object ID : Column("id", "t.id"), TableColumn
    object NAME : Column("name", "t.name"), TableColumn
    object EDITABLE : Column("editable", "t.editable"), TableColumn
    object ENTRY_COUNT : Column("entry_count", "(SELECT COUNT(1) FROM (SELECT DISTINCT e.id FROM entry e LEFT JOIN entry_tag et ON et.entry_id = e.id LEFT JOIN feed_tag ft ON ft.feed_id = e.feed_id WHERE et.tag_id = t.id OR ft.tag_id = t.id))", arrayOf("entry", "entry_tag", "feed_tag", "tag"))
    object UNREAD_ENTRY_COUNT : Column("entry_count", "(SELECT COUNT(1) FROM (SELECT DISTINCT e.id FROM entry e LEFT JOIN entry_tag et ON et.entry_id = e.id LEFT JOIN feed_tag ft ON ft.feed_id = e.feed_id WHERE (e.read_time = 0 OR e.pinned_time) AND (et.tag_id = t.id OR ft.tag_id = t.id)))", arrayOf("entry", "entry_tag", "feed_tag", "tag"))

    interface UpdateCriteria : Repository.UpdateCriteria

    class UpdateTagCriteria(tagId: Long) : UpdateCriteria {
        override val selection = "id = ?"
        override val selectionArgs = arrayOf<Any>(tagId)
    }

    interface DeleteCriteria : Repository.DeleteCriteria

    class DeleteTagCriteria(tagId: Long) : DeleteCriteria {
        override val selection = "id = ? AND editable = 1"
        override val selectionArgs = arrayOf<Any>(tagId)
    }

    interface QueryCriteria : Repository.QueryCriteria<Column> {
        fun config(query: Query.Builder, columns: Array<out Column>)
    }

    class QueryTagCriteria(private val tagId: Long) : QueryCriteria {
        override fun config(query: Query.Builder, columns: Array<out Column>) {
            query.where("t.id = ?", arrayOf(tagId))
        }
    }

    object QueryAllTagsCriteria : QueryCriteria {
        override fun config(query: Query.Builder, columns: Array<out Column>) {
            query.orderBy("t.name")
        }
    }

    object QueryUserTagsCriteria : QueryCriteria {
        override fun config(query: Query.Builder, columns: Array<out Column>) {
            query.where("t.id > 0", emptyArray())
            query.orderBy("t.name")
        }
    }

    object QueryVisibleTagsCriteria : QueryCriteria {
        override fun config(query: Query.Builder, columns: Array<out Column>) {
            query.where("t.id != ?", arrayOf(HIDDEN))
            query.orderBy("(CASE t.id WHEN 0 THEN 0 ELSE 1 END), t.name")
        }
    }

    abstract class QueryHelper<Row>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, Row>(columns) {
        override fun createQuery(criteria: QueryCriteria) = Query.Builder(columns, "tag t")
                .groupBy("t.id")
                .orderBy("t.name")
                .also { criteria.config(it, columns) }
                .create()
    }

}
