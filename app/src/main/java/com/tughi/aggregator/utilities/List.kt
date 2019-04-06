package com.tughi.aggregator.utilities

fun <T> List<T>.has(predicate: (T) -> Boolean): Boolean {
    forEach {
        if (predicate(it)) {
            return true
        }
    }
    return false
}

