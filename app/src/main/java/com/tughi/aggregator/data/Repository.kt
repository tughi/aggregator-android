package com.tughi.aggregator.data

import android.database.Cursor

abstract class Repository<T>(protected val columns: Array<String>, protected val factory: Factory<T>) {

    fun beginTransaction() = Storage.beginTransaction()

    fun setTransactionSuccessful() = Storage.setTransactionSuccessful()

    fun endTransaction() = Storage.endTransaction()

    open class Factory<T> {

        open fun create(cursor: Cursor): T {
            throw UnsupportedOperationException()
        }

    }

}
