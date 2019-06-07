package com.tughi.aggregator.utilities

sealed class Result<out T>
data class Success<T>(val data: T) : Result<T>()
data class Failure(val error: Throwable) : Result<Nothing>()

fun <I, O> Result<I>.then(transform: (I) -> Result<O>) = when (this) {
    is Success -> transform(data)
    is Failure -> Failure(error)
}

fun <T> Result<T>.otherwise(catch: (Throwable) -> Unit) = when (this) {
    is Success -> Unit
    is Failure -> catch(error)
}
