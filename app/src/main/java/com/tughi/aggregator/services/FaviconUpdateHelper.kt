package com.tughi.aggregator.services

import android.database.Cursor
import android.graphics.BitmapFactory
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.utilities.Http
import com.tughi.aggregator.utilities.getOrNull
import com.tughi.aggregator.utilities.toAbsoluteUrl
import java.util.regex.Pattern

object FaviconUpdateHelper {

    suspend fun updateFavicon(feedId: Long) {
        val feed = Feeds.queryOne(Feeds.QueryRowCriteria(feedId), Feed.QueryHelper)
        val feedLink = feed?.link ?: return

        var icon = detectWebsiteFavicon(feedLink)
        if (icon == null) {
            icon = detectRootFavicon(feedLink)
        }

        if (icon?.content != null) {
            Feeds.update(Feeds.UpdateRowCriteria(feedId), Feeds.FAVICON_URL to icon.url, Feeds.FAVICON_CONTENT to icon.content!!)
        }
    }

    private val linkPattern = Pattern.compile("<(body)|<link([^>]+)>")
    private val relIconPattern = Pattern.compile("rel\\s*=\\s*['\"]([sS]hortcut [iI]con|icon)['\"]")
    private val hrefPattern = Pattern.compile("href\\s*=\\s*['\"]([^'\"]+)['\"]")
    private val sizesPattern = Pattern.compile("sizes\\s*=\\s*['\"]([^'\"]+)['\"]")

    private suspend fun detectWebsiteFavicon(feedLink: String): Icon? {
        val response = Http.request(feedLink).getOrNull() ?: return null

        val responseContent = when (val responseBody = response.body()) {
            null -> return null
            else -> responseBody.string()
        }

        val icons = mutableListOf<Icon>()

        // find <head> <link>s
        val linkMatcher = linkPattern.matcher(responseContent)
        while (linkMatcher.find() && linkMatcher.group(1) == null) {
            val attributes = linkMatcher.group(2)
            // is it an icon link?
            val relIconMatcher = relIconPattern.matcher(attributes)
            if (relIconMatcher.find()) {
                val shortcut = relIconMatcher.group(1).toLowerCase().startsWith("shortcut")

                val hrefMatcher = hrefPattern.matcher(attributes)
                // does the link have the required href?
                if (hrefMatcher.find()) {
                    val iconUrl = hrefMatcher.group(1).toAbsoluteUrl(response.request().url().toString())

                    if (shortcut) {
                        icons.add(0, Icon(iconUrl))
                    } else {
                        // check size
                        val sizesMatcher = sizesPattern.matcher(attributes)
                        if (sizesMatcher.find()) {
                            val sizes = sizesMatcher.group(1).trim().toLowerCase()
                            if (sizes == "any" || sizes.split(" ".toRegex()).size > 1) {
                                // multiple sizes are not supported
                                continue
                            }
                        }

                        icons.add(Icon(iconUrl))
                    }
                }
            }
        }

        return downloadIcon(icons)
    }

    private val baseUrlPattern = Pattern.compile("(https?://[^/]+).*")

    private suspend fun detectRootFavicon(url: String): Icon? {
        val matcher = baseUrlPattern.matcher(url)
        if (matcher.matches()) {
            val faviconUrl = matcher.group(1) + "/favicon.ico"

            return downloadIcon(listOf(Icon(faviconUrl)))
        }

        return null
    }

    private suspend fun downloadIcon(icons: List<Icon>): Icon? {
        for (icon in icons) {
            val iconResponse = Http.request(icon.url).getOrNull() ?: continue

            if (iconResponse.isSuccessful) {
                val iconResponseBody = iconResponse.body() ?: continue
                val iconData = iconResponseBody.bytes() ?: continue

                // validate icon
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(iconData, 0, iconData.size, options)
                if (options.outWidth > 0) {
                    return icon.copy(content = iconData)
                }
            }
        }

        return null
    }

    private data class Icon(val url: String, val content: ByteArray? = null) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Icon

            if (url != other.url) return false

            return true
        }

        override fun hashCode(): Int {
            return url.hashCode()
        }
    }

    class Feed(val link: String?) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
                Feeds.LINK
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                    cursor.getString(0)
            )
        }
    }

}
