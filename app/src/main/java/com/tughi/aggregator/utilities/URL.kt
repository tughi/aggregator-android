package com.tughi.aggregator.utilities

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.MalformedURLException
import java.net.URL

fun String.toAbsoluteUrl(baseUrl: String): String {
    val url = trim()
    if (url.startsWith("data:")) {
        return url
    }
    try {
        val absoluteUrl = URL(url)
        return absoluteUrl.toString()
    } catch (exception: MalformedURLException) {
        try {
            val absoluteBaseUrl = URL(baseUrl)
            val absoluteUrl = URL(absoluteBaseUrl, url)
            return absoluteUrl.toString()
        } catch (exception: MalformedURLException) {
            throw IllegalStateException("Failed to make '$url' absolute using '$baseUrl'")
        }
    }
}

private val HttpUrl.hasCredentials
    get() = username.isNotEmpty() || password.isNotEmpty()

/**
 * Copies the credentials of the [baseUrl] into this URL, when both point to the same host, so that
 * feeds discovered on a password protected website inherit its credentials.
 */
fun String.withCredentialsFrom(baseUrl: String): String {
    val base = baseUrl.toHttpUrlOrNull()?.takeIf { it.hasCredentials } ?: return this
    val url = toHttpUrlOrNull() ?: return this
    if (url.hasCredentials || url.host != base.host) {
        return this
    }

    return url.newBuilder()
        .username(base.username)
        .password(base.password)
        .build()
        .toString()
}
