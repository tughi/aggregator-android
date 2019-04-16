package com.tughi.aggregator.data

@Suppress("ClassName")
object MyFeedTags : Repository<MyFeedTags.Column, MyFeedTags.TableColumn, MyFeedTags.UpdateCriteria, MyFeedTags.DeleteCriteria, MyFeedTags.QueryCriteria>("my_feed_tag") {

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("my_feed_tag")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object TAG_ID : Column("tag_id", "mft.tag_id"), TableColumn
    object TAG_NAME : Column("tag_name", "t.name", arrayOf("my_feed_tag", "tag")), TableColumn
    object TYPE : Column("type", "mft.type"), TableColumn

    enum class Type(val value: Int) {
        INCLUDED(0),
        EXCLUDED(1)
    }

    interface UpdateCriteria : Repository.UpdateCriteria

    interface DeleteCriteria : Repository.DeleteCriteria

    class DeleteMyFeedTagCriteria(tagId: Long, type: Type) : DeleteCriteria {
        override val selection: String = "tag_id = ? AND type = ?"
        override val selectionArgs: Array<Any>? = arrayOf(tagId, type.value)
    }

    interface QueryCriteria : Repository.QueryCriteria<Column> {
        fun config(query: Query.Builder, columns: Array<out Column>)
    }

    class QueryMyFeedTagsCriteria(private val type: Type) : QueryCriteria {
        override fun config(query: Query.Builder, columns: Array<out Column>) {
            query.where("mft.type = ?", arrayOf(type.value))
            query.orderBy("t.name")
        }
    }

    abstract class QueryHelper<Row>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, Row>(columns) {
        override fun createQuery(criteria: QueryCriteria) = Query.Builder(columns, "my_feed_tag mft LEFT JOIN tag t ON t.id = mft.tag_id")
                .also { criteria.config(it, columns) }
                .create()
    }

}
