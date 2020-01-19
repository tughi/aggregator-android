package com.tughi.aggregator.data

@Suppress("ClassName")
object EntryTagRules : Repository<EntryTagRules.Column, EntryTagRules.TableColumn, EntryTagRules.UpdateCriteria, EntryTagRules.DeleteCriteria, EntryTagRules.QueryCriteria>("entry_tag_rule") {

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("entry_tag_rule")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object ID : Column("id", "etr.id"), TableColumn
    object FEED_ID : Column("feed_id", "etr.feed_id"), TableColumn
    object FEED_TITLE : Column("feed_title", "COALESCE(f.custom_title, f.title)", arrayOf("feed"))
    object TAG_ID : Column("tag_id", "etr.tag_id"), TableColumn
    object TAG_NAME : Column("tag_name", "t.name", arrayOf("tag"))
    object CONDITION : Column("condition", "etr.condition"), TableColumn

    interface UpdateCriteria : Repository.UpdateCriteria

    interface DeleteCriteria : Repository.DeleteCriteria

    interface QueryCriteria : Repository.QueryCriteria<Column> {
        fun config(query: Query.Builder, columns: Array<out Column>)
    }

    abstract class QueryHelper<Row>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, Row>(columns) {
        override fun createQueryBuilder(criteria: QueryCriteria) = Query.Builder(columns, "entry_tag_rule etr LEFT JOIN feed f ON etr.feed_id = f.id JOIN tag t ON etr.tag_id = t.id")
                .also { criteria.config(it, columns) }
    }

}

class EntryTagRulesQueryCriteria(val feedId: Long) : EntryTagRules.QueryCriteria {
    override fun config(query: Query.Builder, columns: Array<out EntryTagRules.Column>) {
        val selection = "etr.feed_id IS NULL OR etr.feed_id = ?"
        val selectionArgs = listOf<Any?>(feedId)
        query.where(selection, selectionArgs.toTypedArray())
    }
}
