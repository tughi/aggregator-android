package com.tughi.aggregator.feeds

import android.text.Html
import com.tughi.xml.Document
import com.tughi.xml.TagElement
import com.tughi.xml.TextElement
import com.tughi.xml.TypedTextElement
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.SAXException
import java.util.*

/**
 * An unified RSS and Atom parser.
 */
class FeedParser(private val feedUrl: String, private val listener: Listener) {

    val feedContentHandler: ContentHandler

    private val feedDateParser = FeedDateParser()

    private var feedLink: String? = null
    private var feedTitle: String? = null
    private var feedLanguage: String? = null

    private var entryUid: String? = null
    private var entryLink: String? = null
    private var entryTitle: String? = null
    private var entryContent: String? = null
    private var entryAuthor: String? = null
    private var entryPublishDate: Date? = null
    private var entryPublishDateText: String? = null

    init {
        val document = Document()
        val defaultFeedTitle = "Feed"

        // create RSS elements

        val rssUris = arrayOf("", "http://channel.netscape.com/rdf/simple/0.9/", "http://purl.org/rss/1.0/")
        var channelElement = document.addChild(TagElement("rss")).addChild(object : TagElement("channel") {
            override fun end(namespace: String?, name: String?) {
                super.end(namespace, name)

                listener.onParsedFeed(feedLink, feedTitle ?: defaultFeedTitle, feedLanguage)
            }
        })
        val channelLinkElement = channelElement.addChild(object : TextElement("link", *rssUris) {
            override fun handleText(text: String) {
                handleFeedLink(text)
            }
        })
        val channelTitleElement = channelElement.addChild(object : TextElement("title", *rssUris) {
            override fun handleText(text: String?) {
                handleFeedTitle(text)
            }
        })
        channelElement.addChild(object : TextElement("language", "", "http://purl.org/dc/elements/1.1/") {
            override fun handleText(text: String) {
                handleFeedLanguage(text)
            }
        })
        val itemElement = channelElement.addChild(object : TagElement("item", *rssUris) {
            override fun end(uri: String?, name: String?) {
                super.end(uri, name)
                handleEntryEnd()
            }
        })
        itemElement.addChild(object : TextElement("title", *rssUris) {
            override fun handleText(text: String) {
                handleEntryTitle(text)
            }
        })
        itemElement.addChild(object : TextElement("link", *rssUris) {
            override fun handleText(text: String) {
                handleEntryLink(text.trim())
            }
        })
        itemElement.addChild(object : TextElement("guid", *rssUris) {
            override fun handleText(text: String) {
                handleEntryUID(text)
            }
        })
        itemElement.addChild(object : TypedTextElement("description", *rssUris) {
            override fun handleText(text: String, type: String) {
                handleEntryContent(text)
            }
        })
        itemElement.addChild(object : TextElement("pubDate", *rssUris) {
            override fun handleText(text: String) {
                handleEntryDate(text)
            }
        })
        itemElement.addChild(object : TextElement("author", *rssUris) {
            override fun handleText(text: String) {
                var authorText = text
                authorText = authorText.trim()
                val nameIndex = authorText.indexOf('(')
                if (nameIndex >= 0 && authorText.endsWith(")")) {
                    authorText = authorText.substring(nameIndex + 1, authorText.length - 1)
                }
                handleEntryAuthor(authorText)
            }
        })
        itemElement.addChild(object : TextElement("creator", "http://purl.org/dc/elements/1.1/") {
            override fun handleText(text: String) {
                handleEntryAuthor(text)
            }
        })
        itemElement.addChild(object : TextElement("date", "http://purl.org/dc/elements/1.1/") {
            override fun handleText(text: String) {
                handleEntryDate(text)
            }
        })
        itemElement.addChild(object : TextElement("encoded", "http://purl.org/rss/1.0/modules/content/") {
            override fun handleText(text: String) {
                handleEntryContent(text)
            }
        })
        itemElement.addChild(object : TextElement("origLink", "http://rssnamespace.org/feedburner/ext/1.0") {
            override fun handleText(text: String) {
                handleEntryLink(text.trim())
            }
        })

        // create RDF elements

        val rdfElement = document.addChild(TagElement("RDF", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"))
        channelElement = rdfElement.addChild(object : TagElement("channel", *rssUris) {
            override fun end(namespace: String?, name: String?) {
                super.end(namespace, name)

                listener.onParsedFeed(feedLink, feedTitle ?: defaultFeedTitle, feedLanguage)
            }
        })
        channelElement.addChild(channelLinkElement)
        channelElement.addChild(channelTitleElement)
        channelElement.addChild(object : TextElement("language", "http://purl.org/dc/elements/1.1/") {
            override fun handleText(text: String) {
                handleFeedLanguage(text)
            }
        })
        rdfElement.addChild(itemElement)

        // create Atom elements
        val atomUris = arrayOf("http://www.w3.org/2005/Atom", "http://purl.org/atom/ns#")
        val feedElement = document.addChild(object : TagElement("feed", *atomUris) {
            override fun start(namespace: String?, name: String?, attributes: Attributes?) {
                super.start(namespace, name, attributes)
                handleFeedLanguage(attributes!!.getValue("lang"))
            }

            override fun end(namespace: String?, name: String?) {
                super.end(namespace, name)

                listener.onParsedFeed(feedLink, feedTitle ?: defaultFeedTitle, feedLanguage)
            }
        })
        feedElement.addChild(object : TypedTextElement("title", *atomUris) {
            override fun handleText(text: String?, type: String) {
                handleFeedTitle(text)
            }
        })
        feedElement.addChild(object : TagElement("link", *atomUris) {
            override fun start(namespace: String?, name: String?, attributes: Attributes?) {
                if (attributes != null && "alternate" == attributes.getValue("rel")) {
                    handleFeedLink(attributes.getValue("href"))
                }
            }
        })
        val entryElement = feedElement.addChild(object : TagElement("entry", *atomUris) {
            override fun end(uri: String?, name: String?) {
                super.end(uri, name)
                handleEntryEnd()
            }
        })
        entryElement.addChild(object : TypedTextElement("title", *atomUris) {
            override fun handleText(text: String, type: String) {
                handleEntryTitle(text)
            }
        })
        entryElement.addChild(object : TagElement("link", *atomUris) {
            override fun start(uri: String?, name: String?, attributes: Attributes?) {
                super.start(uri, name, attributes)
                if (attributes != null) {
                    val rel = attributes.getValue("rel")
                    if (rel == null || rel == "alternate") {
                        handleEntryLink(attributes.getValue("href"))
                    }
                }
            }
        })
        entryElement.addChild(object : TypedTextElement("id", *atomUris) {
            override fun handleText(text: String, type: String) {
                handleEntryUID(text)
            }
        })
        entryElement.addChild(object : TypedTextElement("summary", *atomUris) {
            override fun handleText(text: String, type: String) {
                handleEntryContent(text)
            }
        })
        entryElement.addChild(object : TypedTextElement("content", *atomUris) {
            override fun handleText(text: String, type: String) {
                handleEntryContent(text)
            }
        })
        entryElement.addChild(object : TextElement("published", *atomUris) {
            override fun handleText(text: String) {
                handleEntryDate(text)
            }
        })
        entryElement.addChild(object : TextElement("updated", *atomUris) {
            override fun handleText(text: String) {
                handleEntryDate(text)
            }
        })
        entryElement.addChild(object : TextElement("issued", "http://purl.org/atom/ns#") {
            override fun handleText(text: String) {
                handleEntryDate(text)
            }
        })
        entryElement.addChild(TagElement("author", *atomUris)).addChild(object : TextElement("name", *atomUris) {
            override fun handleText(text: String) {
                handleEntryAuthor(text)
            }
        })

        val mediaDescription = StringBuilder()
        val mediaGroupElement = entryElement.addChild(object : TagElement("group", "http://search.yahoo.com/mrss/") {
            override fun start(namespace: String?, name: String?, attributes: Attributes?) {
                mediaDescription.setLength(0)
            }

            override fun end(namespace: String?, name: String?) {
                handleEntryContent(mediaDescription.toString())
            }
        })
        mediaGroupElement.addChild(object : TypedTextElement("description", "http://search.yahoo.com/mrss/") {
            override fun handleText(text: String, type: String) {
                if ("text" == type || "plain" == type) {
                    mediaDescription.append("<p>")
                    mediaDescription.append(text.replace("\r\n|\n|\r".toRegex(), "<br>"))
                    mediaDescription.append("</p>\n")
                } else {
                    mediaDescription.append(text)
                }
            }
        })
        mediaGroupElement.addChild(object : TagElement("thumbnail", "http://search.yahoo.com/mrss/") {
            override fun start(namespace: String?, name: String?, attributes: Attributes?) {
                if (attributes != null) {
                    mediaDescription.append("<p>")
                    mediaDescription.append("<img src=\"").append(attributes.getValue("url")).append("\" />")
                    mediaDescription.append("</p>\n")
                }
            }
        })

        feedContentHandler = document.contentHandler
    }

    open class Listener {
        open fun onParsedFeed(
                link: String?,
                title: String,
                language: String?
        ) {
        }

        open fun onParsedEntry(
                uid: String,
                link: String?,
                title: String?,
                content: String?,
                author: String?,
                publishDate: Date?,
                publishDateText: String?
        ) {
        }
    }

    private fun handleFeedLink(text: String?) {
        if (text != null) {
            var link = text
            if (link.startsWith("//")) {
                link = feedUrl.substring(0, feedUrl.indexOf("://") + 1) + link
            }
            feedLink = link
        }
    }

    private fun handleFeedTitle(text: String?) {
        feedTitle = text?.trim()
    }

    private fun handleFeedLanguage(text: String?) {
        if (text != null) {
            val language = text.trim()
            if (language.isNotEmpty()) {
                feedLanguage = language
            }
        }
    }

    private fun handleEntryUID(text: String?) {
        if (text != null) {
            val uid = text.trim()
            if (!uid.isEmpty()) {
                entryUid = uid
            }
        }
    }

    private fun handleEntryLink(text: String?) {
        entryLink = text // TODO: convert to absolute URL
    }

    private fun handleEntryTitle(text: String) {
        var title = text
        title = Html.fromHtml(title).toString()
        title = title.trim()
        entryTitle = title
    }

    private fun handleEntryContent(content: String) {
        val fixedContent = fixContent(content)
        val currentEntryContent = entryContent
        if (currentEntryContent == null || (fixedContent != null && fixedContent.length > currentEntryContent.length)) {
            entryContent = fixedContent
        }
    }

    // private val relativeLinkPattern = Pattern.compile("=\\s*[\'\"]/")

    private fun fixContent(text: String?): String? {
        /* TODO: convert relative to absolute URLs
        val feedLink = feedLink
        if (text != null && feedLink != null) {
            var description = text
            if (relativeLinkPattern.matcher(description).find()) {
                val feedBaseUrl = feedLink // T O D O: get base url from feedLink
                // T O D O: optimize regex replace to use groups
                description = description.replace("[hH][rR][eE][fF]\\s*=\\s*'/".toRegex(), "href='$feedBaseUrl")
                description = description.replace("[hH][rR][eE][fF]\\s*=\\s*\"/".toRegex(), "href=\"$feedBaseUrl")
                description = description.replace("[sS][rR][cC]\\s*=\\s*'/".toRegex(), "src='$feedBaseUrl")
                description = description.replace("[sS][rR][cC]\\s*=\\s*\"/".toRegex(), "src=\"$feedBaseUrl")
                // T O D O: check this optimization
                description = description.replace("([hH][rR][eE][fF]|[sS][rR][cC])\\s*=\\s*(['\"])/".toRegex(), "\$1=\$2$feedBaseUrl")
            }
            return description
        }
        */
        return text
    }

    private fun handleEntryAuthor(text: String?) {
        if (text != null) {
            val author = text.trim()
            if (!author.isEmpty()) {
                entryAuthor = author
            }
        }
    }

    private fun handleEntryDate(text: String?) {
        if (text != null) {
            val date = feedDateParser.parse(text)
            if (date != null) {
                entryPublishDate = date
            }
        }

        entryPublishDateText = text
    }

    private fun handleEntryEnd() {
        var digest: ByteArray? = null
        val uid = entryUid
        if (uid != null) {
            digest = DigestUtils.md5(uid)
        } else {
            val title = entryTitle
            if (title != null) {
                digest = DigestUtils.md5(title)
            } else {
                val content = entryContent
                if (content != null && !content.isEmpty()) {
                    digest = DigestUtils.md5(content)
                }
            }
        }

        if (digest != null) {
            entryUid = String(Hex.encodeHex(digest))
        }

        val entryUid = entryUid
        if (entryUid != null) {
            listener.onParsedEntry(
                    entryUid,
                    entryLink,
                    entryTitle,
                    entryContent,
                    entryAuthor,
                    entryPublishDate,
                    entryPublishDateText
            )
        } else {
            throw SAXException("Could not generate UID for an entry")
        }

        this.entryUid = null
        this.entryLink = null
        this.entryTitle = null
        this.entryContent = null
        this.entryAuthor = null
        this.entryPublishDate = null
        this.entryPublishDateText = null
    }

}
