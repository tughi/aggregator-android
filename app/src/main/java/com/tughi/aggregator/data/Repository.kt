package com.tughi.aggregator.data

import android.content.ContentValues
import android.database.Cursor

abstract class Repository<T>(protected val columns: Array<String>, protected val mapper: DataMapper<T>) {

    open class DataMapper<T> {

        open fun map(cursor: Cursor): T {
            throw UnsupportedOperationException()
        }

        open fun map(data: T): ContentValues {
            throw UnsupportedOperationException()
        }

    }

}
