package com.tughi.aggregator.data

import android.database.Cursor

interface DataMapper<T> {

    fun map(cursor: Cursor): T

}
