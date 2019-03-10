package com.tughi.aggregator.data

import android.content.ContentValues

fun Array<out Pair<String, Any?>>.toContentValues() = ContentValues(size).also {
    for ((column, value) in this) {
        when (value) {
            null -> it.putNull(column)
            is ByteArray -> it.put(column, value)
            is Int -> it.put(column, value)
            is Long -> it.put(column, value)
            is String -> it.put(column, value)
            is UpdateMode -> it.put(column, value.serialize())
            else -> throw UnsupportedOperationException("Cannot convert type: ${value.javaClass}")
        }
    }
}
