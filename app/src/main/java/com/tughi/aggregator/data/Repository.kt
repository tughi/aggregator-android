package com.tughi.aggregator.data

import android.content.ContentValues
import android.database.Cursor

abstract class Repository<T>(protected val columns: Array<String>, protected val mapper: DataMapper<T>) {

    fun beginTransaction() = Storage.beginTransaction()

    fun setTransactionSuccessful() = Storage.setTransactionSuccessful()

    fun endTransaction() = Storage.endTransaction()

    open class DataMapper<T> {

        open fun map(cursor: Cursor): T {
            throw UnsupportedOperationException()
        }

        fun map(data: Array<out Pair<String, Any?>>) = ContentValues(data.size).apply {
            for ((column, value) in data) {
                put(column, value)
            }
        }

        private fun ContentValues.put(column: String, value: Any?): Unit = when (value) {
            null -> putNull(column)
            is ByteArray -> put(column, value)
            is Int -> put(column, value)
            is Long -> put(column, value)
            is String -> put(column, value)
            is UpdateMode -> put(column, value.serialize())
            else -> throw UnsupportedOperationException("Cannot convert type: ${value.javaClass}")
        }

    }

}
