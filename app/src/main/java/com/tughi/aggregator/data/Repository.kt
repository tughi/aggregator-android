package com.tughi.aggregator.data

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteQueryBuilder

abstract class Repository {

    fun beginTransaction() = Storage.beginTransaction()

    fun setTransactionSuccessful() = Storage.setTransactionSuccessful()

    fun endTransaction() = Storage.endTransaction()

    fun <T> query(id: Long, factory: Factory<T>): T? {
        val query = createQueryBuilder(factory.columns)
                .selection(createQueryOneSelection(), arrayOf(id))
                .create()

        Storage.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                return factory.create(cursor)
            }
        }

        return null
    }

    abstract fun createQueryOneSelection(): String

    fun <T> query(criteria: Criteria, factory: Factory<T>): List<T> {
        val query = createQueryBuilder(factory.columns)
                .also { criteria.init(it) }
                .create()

        Storage.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val entries = mutableListOf<T>()

                do {
                    entries.add(factory.create(cursor))
                } while (cursor.moveToNext())

                return entries
            }
        }

        return emptyList()
    }

    abstract fun createQueryBuilder(columns: Array<String>): SupportSQLiteQueryBuilder

    interface Criteria {

        fun init(builder: SupportSQLiteQueryBuilder)

    }

    abstract class Factory<T> {

        abstract val columns: Array<String>

        open fun create(cursor: Cursor): T {
            throw UnsupportedOperationException()
        }

    }

}
