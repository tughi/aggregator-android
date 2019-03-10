package com.tughi.aggregator.data

import android.database.Cursor

abstract class Repository<T>(protected val factory: Factory<T>) {

    fun beginTransaction() = Storage.beginTransaction()

    fun setTransactionSuccessful() = Storage.setTransactionSuccessful()

    fun endTransaction() = Storage.endTransaction()

    abstract class Factory<T> {

        abstract val columns: Array<String>

        open fun create(cursor: Cursor): T {
            throw UnsupportedOperationException()
        }

    }

}
