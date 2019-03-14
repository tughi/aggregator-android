package com.tughi.aggregator.data

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteQuery

abstract class Repository<Column : Repository.Column, TableColumn : Repository.TableColumn, UpdateCriteria : Repository.UpdateCriteria, DeleteCriteria : Repository.DeleteCriteria, QueryCriteria : Repository.QueryCriteria<Column>>(val tableName: String) {

    fun insert(vararg data: Pair<TableColumn, Any?>): Long = Database.insert(tableName, data.toContentValues())

    fun update(criteria: UpdateCriteria, vararg data: Pair<TableColumn, Any?>) = Database.update(tableName, data.toContentValues(), criteria.selection, criteria.selectionArgs, criteria.affectedRowId)

    fun delete(criteria: DeleteCriteria) = Database.delete(tableName, criteria.selection, criteria.selectionArgs)

    fun <Row> query(criteria: QueryCriteria, helper: QueryHelper<Column, QueryCriteria, Row>): List<Row> {
        val query = helper.createQuery(criteria)

        Database.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val entries = mutableListOf<Row>()

                do {
                    entries.add(helper.createRow(cursor))
                } while (cursor.moveToNext())

                return entries
            }
        }

        return emptyList()
    }

    fun <Row> liveQuery(criteria: QueryCriteria, helper: QueryHelper<Column, QueryCriteria, Row>) = Database.createLiveData(helper.observedTables) { query(criteria, helper) }

    fun <Row> queryOne(criteria: QueryCriteria, helper: QueryHelper<Column, QueryCriteria, Row>): Row? {
        val query = helper.createQuery(criteria)

        Database.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                return helper.createRow(cursor)
            }
        }

        return null
    }

    fun <Row> liveQueryOne(criteria: QueryCriteria, helper: QueryHelper<Column, QueryCriteria, Row>) = Database.createLiveData(helper.observedTables) { queryOne(criteria, helper) }

    open class Column(val name: String, val projection: String, val projectionTables: Array<String>)

    interface TableColumn {
        val name: String
    }

    interface UpdateCriteria {
        val affectedRowId: Any?

        val selection: String?
        val selectionArgs: Array<Any>?
    }

    interface DeleteCriteria {
        val selection: String?
        val selectionArgs: Array<Any>?
    }

    interface QueryCriteria<Column>

    abstract class QueryHelper<Column : Repository.Column, QueryCriteria : Repository.QueryCriteria<Column>, Row>(val columns: Array<out Column>) {

        internal val observedTables: Array<Database.ObservedTable>

        init {
            val tables = mutableSetOf<String>()
            for (column in columns) {
                tables.addAll(column.projectionTables)
            }
            observedTables = tables.map { Database.ObservedTable(it) }.toTypedArray()
        }

        abstract fun createQuery(criteria: QueryCriteria): SupportSQLiteQuery

        abstract fun createRow(cursor: Cursor): Row

    }

}
