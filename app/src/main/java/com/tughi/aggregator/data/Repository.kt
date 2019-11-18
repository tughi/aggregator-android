package com.tughi.aggregator.data

import android.database.Cursor

abstract class Repository<Column : Repository.Column, TableColumn : Repository.TableColumn, UpdateCriteria : Repository.UpdateCriteria, DeleteCriteria : Repository.DeleteCriteria, QueryCriteria : Repository.QueryCriteria<Column>>(val tableName: String) {

    fun insert(vararg data: Pair<TableColumn, Any?>): Long = Database.insert(tableName, data.toContentValues())

    fun update(criteria: UpdateCriteria, vararg data: Pair<TableColumn, Any?>) = Database.update(tableName, data.toContentValues(), criteria.selection, criteria.selectionArgs)

    fun delete(criteria: DeleteCriteria) = Database.delete(tableName, criteria.selection, criteria.selectionArgs)

    fun <Row> query(criteria: QueryCriteria, helper: QueryHelper<Column, QueryCriteria, Row>) = Database.query(helper.createQuery(criteria), helper::transform)

    fun <Row> liveQuery(criteria: QueryCriteria, helper: QueryHelper<Column, QueryCriteria, Row>) = Database.liveQuery(helper.createQuery(criteria), helper::transform)

    fun <Row> queryOne(criteria: QueryCriteria, helper: QueryHelper<Column, QueryCriteria, Row>) = Database.query(helper.createQuery(criteria), helper::transformOne)

    fun <Row> liveQueryOne(criteria: QueryCriteria, helper: QueryHelper<Column, QueryCriteria, Row>) = Database.liveQuery(helper.createQuery(criteria), helper::transformOne)

    fun <Row> queryCount(criteria: QueryCriteria, helper: QueryHelper<Column, QueryCriteria, Row>) = Database.query(helper.createQueryCount(criteria), helper::transformCount)

    fun <Row> liveQueryCount(criteria: QueryCriteria, helper: QueryHelper<Column, QueryCriteria, Row>) = Database.liveQuery(helper.createQueryCount(criteria), helper::transformCount)

    open class Column(val name: String, val projection: String, val projectionTables: Array<String>)

    interface TableColumn {
        val name: String
    }

    interface UpdateCriteria {
        val selection: String?
        val selectionArgs: Array<Any>?
    }

    interface DeleteCriteria {
        val selection: String?
        val selectionArgs: Array<Any>?
    }

    interface QueryCriteria<Column>

    abstract class QueryHelper<Column : Repository.Column, QueryCriteria : Repository.QueryCriteria<Column>, Row>(val columns: Array<out Column>) {

        fun createQuery(criteria: QueryCriteria) = createQueryBuilder(criteria).build()

        fun createQueryCount(criteria: QueryCriteria) = createQueryBuilder(criteria).buildCount()

        abstract fun createQueryBuilder(criteria: QueryCriteria): Query.Builder

        abstract fun createRow(cursor: Cursor): Row

        internal fun transform(cursor: Cursor): List<Row> {
            if (cursor.moveToFirst()) {
                val entries = mutableListOf<Row>()
                do {
                    entries.add(createRow(cursor))
                } while (cursor.moveToNext())
                return entries
            }
            return emptyList()
        }

        internal fun transformOne(cursor: Cursor): Row? {
            if (cursor.moveToFirst()) {
                return createRow(cursor)
            }
            return null
        }

        internal fun transformCount(cursor: Cursor): Int {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0)
            }
            return 0
        }

    }

}
