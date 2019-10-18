package com.tughi.aggregator.data

@Suppress("ClassName")
object Tags : Repository<Tags.Column, Tags.TableColumn, Tags.UpdateCriteria, Tags.DeleteCriteria, Tags.QueryCriteria>("tag") {

    const val ALL = 0L
    const val STARRED = 1L

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("tag")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object ID : Column("id", "t.id"), TableColumn
    object NAME : Column("name", "t.name"), TableColumn
    object EDITABLE : Column("editable", "t.editable"), TableColumn
    object ENTRY_COUNT : Column("entry_count", "(SELECT COUNT(1) FROM entry_fts WHERE tags MATCH t.id)", arrayOf("entry", "entry_tag", "feed_tag", "tag"))
    object UNREAD_ENTRY_COUNT : Column("entry_count", "(SELECT COUNT(1) FROM entry_fts ef LEFT JOIN entry e ON ef.docid = e.id WHERE ef.tags MATCH t.id AND (e.read_time = 0 OR e.pinned_time))", arrayOf("entry", "entry_tag", "feed_tag", "tag"))

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
            query.where("t.id >= $STARRED", emptyArray())
            query.orderBy("(CASE WHEN t.id > $STARRED THEN 1 ELSE 0 END), t.name")
        }
    }

    object QueryVisibleTagsCriteria : QueryCriteria {
        override fun config(query: Query.Builder, columns: Array<out Column>) {
            query.orderBy("(CASE WHEN t.id > $STARRED THEN 1 ELSE 0 END), t.name")
        }
    }

    abstract class QueryHelper<Row>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, Row>(columns) {
        override fun createQueryBuilder(criteria: QueryCriteria) = Query.Builder(columns, "tag t")
                .groupBy("t.id")
                .orderBy("t.name")
                .also { criteria.config(it, columns) }
    }

}
