package com.tughi.aggregator.utilities

private val URL_REGEX = "^((.+)://([^/]+))(.*)".toRegex()

fun String.toAbsoluteUrl(baseUrl: String): String {
    val url = trim()
    if (url.startsWith("/")) {
        val rootUrl = baseUrl.replace(URL_REGEX, "\$1")
        return rootUrl + url
    }
    if (!url.matches(URL_REGEX)) {
        if (baseUrl.endsWith("/")) {
            return baseUrl + url
        } else {
            TODO("Cannot create absolute feed URL")
        }
    }
    return url
}
