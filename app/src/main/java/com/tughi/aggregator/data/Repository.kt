package com.tughi.aggregator.data

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteQueryBuilder

abstract class Repository<TC : Repository.TableColumn, QC : Repository.Column> {

    protected abstract val tableName: String

    fun insert(vararg data: Pair<TC, Any?>): Long = Database.insert(tableName, data.toContentValues())

    fun update(id: Long, vararg data: Pair<TC, Any?>) = Database.update(tableName, data.toContentValues(), "id = ?", arrayOf(id), id)

    fun update(selection: String?, selectionArgs: Array<Any>?, vararg data: Pair<TC, Any?>) = Database.update(tableName, data.toContentValues(), selection, selectionArgs)

    fun <R> query(id: Long, factory: Factory<QC, R>): R? {
        val query = createQueryBuilder(factory.columns)
                .selection(createQueryOneSelection(), arrayOf(id))
                .create()

        Database.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                return factory.create(cursor)
            }
        }

        return null
    }

    abstract fun createQueryOneSelection(): String

    fun <T> liveQuery(id: Long, factory: Factory<QC, T>) = Database.createLiveData(tableName) { query(id, factory) }

    fun <R> query(criteria: Criteria, factory: Factory<QC, R>): List<R> {
        val query = createQueryBuilder(factory.columns)
                .also { criteria.init(it) }
                .create()

        Database.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val entries = mutableListOf<R>()

                do {
                    entries.add(factory.create(cursor))
                } while (cursor.moveToNext())

                return entries
            }
        }

        return emptyList()
    }

    abstract fun createQueryBuilder(columns: Array<QC>): SupportSQLiteQueryBuilder

    fun <T> liveQuery(criteria: Criteria, factory: Factory<QC, T>) = Database.createLiveData(tableName) { query(criteria, factory) }

    interface Criteria {

        fun init(builder: SupportSQLiteQueryBuilder)

    }

    interface Column {
        val name: String
    }

    interface TableColumn : Column

    abstract class Factory<C, R> {

        abstract val columns: Array<C>

        open fun create(cursor: Cursor): R {
            throw UnsupportedOperationException()
        }

    }

}
