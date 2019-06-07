package com.tughi.aggregator.utilities

sealed class Result<out T>
data class Success<T>(val data: T) : Result<T>()
data class Failure(val cause: Throwable) : Result<Nothing>()

fun <I, O> Result<I>.then(transform: (I) -> Result<O>) = when (this) {
    is Success -> transform(data)
    is Failure -> this
}

fun <T> Result<T>.onFailure(catch: (Throwable) -> Unit) = when (this) {
    is Success -> Unit
    is Failure -> catch(cause)
}

fun <T> Result<T>.getOrNull() = when (this) {
    is Success -> data
    is Failure -> null
}