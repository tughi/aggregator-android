package com.tughi.aggregator.data

@Suppress("ClassName")
object FeedTags : Repository<FeedTags.Column, FeedTags.TableColumn, FeedTags.UpdateCriteria, FeedTags.DeleteCriteria, FeedTags.QueryCriteria>("feed_tag") {

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("feed_tag")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object FEED_ID : Column("feed_id", "ft.entry_id"), TableColumn
    object TAG_ID : Column("tag_id", "ft.tag_id"), TableColumn
    object TAG_NAME : Column("tag_name", "t.name", arrayOf("feed_tag", "tag")), TableColumn
    object TAG_TIME : Column("tag_time", "ft.tag_time"), TableColumn

    interface UpdateCriteria : Repository.UpdateCriteria

    interface DeleteCriteria : Repository.DeleteCriteria

    class DeleteFeedTagCriteria(feedId: Long, tagId: Long) : DeleteCriteria {
        override val selection: String = "feed_id = ? AND tag_id = ?"
        override val selectionArgs: Array<Any>? = arrayOf(feedId, tagId)
    }

    interface QueryCriteria : Repository.QueryCriteria<Column> {
        fun config(query: Query.Builder, columns: Array<out Column>)
    }

    class QueryFeedTagsCriteria(private val feedId: Long) : QueryCriteria {
        override fun config(query: Query.Builder, columns: Array<out Column>) {
            query.where("ft.feed_id = ?", arrayOf(feedId))
        }
    }

    abstract class QueryHelper<Row>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, Row>(columns) {
        override fun createQuery(criteria: QueryCriteria) = Query.Builder(columns, "feed_tag ft LEFT JOIN tag t ON t.id = ft.tag_id")
                .also { criteria.config(it, columns) }
                .create()
    }

}
