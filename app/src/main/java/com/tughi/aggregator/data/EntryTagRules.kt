package com.tughi.aggregator.data

@Suppress("ClassName")
object EntryTagRules : Repository<EntryTagRules.Column, EntryTagRules.TableColumn, EntryTagRules.UpdateCriteria, EntryTagRules.DeleteCriteria, EntryTagRules.QueryCriteria>("entry_tag_rule") {

    open class Column(name: String, projection: String, projectionTables: Array<String> = arrayOf("entry_tag_rule")) : Repository.Column(name, projection, projectionTables)
    interface TableColumn : Repository.TableColumn

    object ID : Column("id", "etr.id"), TableColumn
    object FEED_ID : Column("feed_id", "etr.feed_id"), TableColumn
    object TAG_ID : Column("tag_id", "etr.tag_id"), TableColumn
    object CONDITION : Column("condition", "etr.condition"), TableColumn

    interface UpdateCriteria : Repository.UpdateCriteria

    interface DeleteCriteria : Repository.DeleteCriteria

    interface QueryCriteria : Repository.QueryCriteria<Column> {
        fun config(query: Query.Builder, columns: Array<out Column>)
    }

    abstract class QueryHelper<Row>(vararg columns: Column) : Repository.QueryHelper<Column, QueryCriteria, Row>(columns) {
        override fun createQueryBuilder(criteria: QueryCriteria) = Query.Builder(columns, "entry_tag_rule etr")
                .also { criteria.config(it, columns) }
    }

}
