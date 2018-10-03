package com.tughi.aggregator.feeds

import com.tughi.aggregator.data.Feed
import okhttp3.MediaType
import java.io.CharArrayWriter
import java.io.Reader
import java.util.*
import java.util.regex.Pattern

class FeedsFinder(private val content: Reader, private val contentType: MediaType?, private val url: String?) {

    private val feeds = ArrayList<Feed>()

    fun find(): List<Feed> {
        assert(content.markSupported())

        with(contentType ?: detectContentType(content)) {
            val type = "${type()}/${subtype()}"
            when (type) {
                "application/json" -> {
                    TODO("Validate JSON feed")
                }
                "text/html" -> {
                    searchHtmlForFeeds(content)
                }
                else -> {
                    throw IllegalStateException("Unsupported content type: $type")
                }
            }
        }

        return feeds
    }

    private fun detectContentType(content: Reader): MediaType {
        TODO("Detect content type")
    }

    private fun searchHtmlForFeeds(content: Reader) {
        val attributePattern = Pattern.compile("(\\b\\w+\\b)\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\"'<>\\s]+)")

        do {
            val linkAttributes = findLinkTag(content) ?: break

            val attributes = HashMap<String, String>()
            val attributeMatcher = attributePattern.matcher(linkAttributes)
            while (attributeMatcher.find()) {
                val name = attributeMatcher.group(1)
                var value = attributeMatcher.group(2)
                if (value[0] == '"' || value[0] == '\'') {
                    value = value.substring(1, value.length - 1)
                }
                attributes[name] = value
            }

            if (attributes["rel"] == "alternate") {
                val type = attributes["type"]
                if (type == "application/rss+xml" || type == "application/atom+xml") {
                    val href = attributes["href"]
                    if (href != null) {
                        feeds.add(Feed(
                                url = href,
                                title = attributes["title"] ?: "Feed"
                        ))
                        // TODO: addFeed(attributes.get("title"), fixLink(attributes.get("href")), link)
                    }
                }
            }
        } while (true)
    }

    private fun findLinkTag(content: Reader): String? {
        val buffer = CharArrayWriter(500)

        var state = 0
        val chars = CharArray(1)
        loop@ do {
            if (content.read(chars, 0, 1) == -1) {
                return null
            }
            val char = chars[0]

            when (state) {
                0 -> {
                    state = if (char == '<') 1 else 0
                }
                1 -> {
                    buffer.reset()
                    buffer.append('<')
                    buffer.append(char)
                    if (!char.isWhitespace()) {
                        state = if (char == 'l' || char == 'L') 2 else 0
                    }
                }
                2 -> {
                    buffer.append(char)
                    state = if (char == 'i' || char == 'I') 3 else 0
                }
                3 -> {
                    buffer.append(char)
                    state = if (char == 'n' || char == 'N') 4 else 0
                }
                4 -> {
                    buffer.append(char)
                    state = if (char == 'k' || char == 'K') 5 else 0
                }
                5 -> {
                    buffer.append(char)
                    state = if (char.isWhitespace()) 6 else 0
                }
                6 -> {
                    buffer.append(char)
                    if (char == '>') {
                        break@loop
                    }
                }
            }
        } while (buffer.size() < 10000)

        return if (state != 6) null else buffer.toString()
    }

/*
    private fun addFeed(title: String?, href: String?) {
        var title = title
        if (href == null) {
            return
        }

        if (title == null) {
            val start = href.indexOf("://") + 3
            val end = href.indexOf('/', start)
            title = href.substring(start, end)
        }

        feeds.add(Feed(href, Html.fromHtml(title).toString()))
    }
*/

}
