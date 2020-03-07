package com.tughi.aggregator.feeds

import android.util.Xml
import com.tughi.aggregator.utilities.toAbsoluteUrl
import java.io.BufferedReader
import java.io.CharArrayWriter
import java.io.Reader
import java.util.HashMap
import java.util.regex.Pattern

class FeedsFinder(private val listener: Listener) {

    fun find(content: Reader, contentUrl: String) {
        assert(content.markSupported())

        val feedParser = FeedParser(contentUrl, object : FeedParser.Listener() {
            override fun onParsedFeed(title: String, link: String?, language: String?) {
                listener.onFeedFound(url = contentUrl, title = title, link = link)
            }
        })

        val bufferSize = 4096
        val reader = NotClosableReader(content, bufferSize)
        try {
            reader.mark(bufferSize)
            Xml.parse(reader, feedParser.feedContentHandler)
        } catch (exception: Exception) {
            reader.reset()
            searchHtmlForFeeds(reader, contentUrl)
        }
    }

    private fun searchHtmlForFeeds(content: Reader, contentUrl: String) {
        val attributePattern = Pattern.compile("(\\b\\w+\\b)\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\"'<>\\s]+)")

        do {
            val linkAttributes = findLinkTag(content) ?: break

            val attributes = HashMap<String, String>()
            val attributeMatcher = attributePattern.matcher(linkAttributes)
            while (attributeMatcher.find()) {
                val name = attributeMatcher.group(1) ?: throw IllegalStateException()
                var value = attributeMatcher.group(2) ?: throw IllegalStateException()
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
                        listener.onFeedFound(
                                url = href.toAbsoluteUrl(contentUrl),
                                title = attributes["title"] ?: "Untitled feed",
                                link = contentUrl
                        )
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

    interface Listener {
        fun onFeedFound(url: String, title: String, link: String?)
    }

    private class NotClosableReader(reader: Reader, bufferSize: Int) : BufferedReader(reader, bufferSize) {
        override fun close() {
            // ignored
        }
    }

}
