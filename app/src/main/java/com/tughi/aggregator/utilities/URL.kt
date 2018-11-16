package com.tughi.aggregator.utilities

import java.net.MalformedURLException
import java.net.URL

fun String.toAbsoluteUrl(baseUrl: String): String {
    val url = trim()
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
