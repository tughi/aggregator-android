package com.tughi.aggregator.data

import android.database.Cursor

abstract class Repository<T>(protected val columns: Array<String>, protected val mapper: DataMapper<T>) {

    fun beginTransaction() = Storage.beginTransaction()

    fun setTransactionSuccessful() = Storage.setTransactionSuccessful()

    fun endTransaction() = Storage.endTransaction()

    open class DataMapper<T> {

        open fun map(cursor: Cursor): T {
            throw UnsupportedOperationException()
        }

    }

}
