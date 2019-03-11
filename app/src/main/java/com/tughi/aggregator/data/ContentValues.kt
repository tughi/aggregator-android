package com.tughi.aggregator.data

import android.content.ContentValues

fun <TC : Repository.TableColumn> Array<out Pair<TC, Any?>>.toContentValues() = ContentValues(size).also {
    for ((column, value) in this) {
        when (value) {
            null -> it.putNull(column.name)
            is ByteArray -> it.put(column.name, value)
            is Int -> it.put(column.name, value)
            is Long -> it.put(column.name, value)
            is String -> it.put(column.name, value)
            is UpdateMode -> it.put(column.name, value.serialize())
            else -> throw UnsupportedOperationException("Cannot convert type: ${value.javaClass}")
        }
    }
}
