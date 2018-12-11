package com.tughi.aggregator.feeds

import android.util.Xml
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.UpdateMode
import com.tughi.xml.Document
import com.tughi.xml.Element
import com.tughi.xml.TagElement
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStream
import java.util.*

object OpmlParser {

    fun parse(inputStream: InputStream, listener: Listener) {
        val document = Document()
        val opmlElement = document.addChild(TagElement("opml"))
        val bodyElement = opmlElement.addChild(TagElement("body"))

        bodyElement.addChild(object : TagElement("outline") {
            private val path = LinkedList<String>()

            override fun getChild(uri: String, name: String): Element? = if ("outline" == name) this else null

            override fun start(uri: String?, name: String?, attributes: Attributes?) {
                val text = attributes!!.getValue("text")

                var type: String? = attributes.getValue("type")
                if (type == null && attributes.getValue("xmlUrl") != null) {
                    type = "rss"
                }

                if (type == "rss") {
                    val feedLink = attributes.getValue("htmlUrl")
                    var feedCustomTitle = text
                    var feedTitle = attributes.getValue("title")
                    if (feedTitle == null) {
                        if (feedCustomTitle == null) {
                            feedTitle = "Feed"
                            feedCustomTitle = feedTitle
                        } else {
                            feedTitle = feedCustomTitle
                        }
                    } else if (feedTitle == feedCustomTitle) {
                        feedCustomTitle = null
                    }
                    val feedUrl = attributes.getValue("xmlUrl")
                    val feedUpdateMode = attributes.getValue("updateMode").let {
                        if (it == null) {
                            return@let DefaultUpdateMode
                        }

                        return@let UpdateMode.deserialize(it)
                    }
                    val feedCategory = if (path.isEmpty()) null else path[0]

                    listener.onFeedParsed(
                            url = feedUrl,
                            title = feedTitle,
                            link = feedLink,
                            category = feedCategory,
                            customTitle = feedCustomTitle,
                            updateMode = feedUpdateMode
                    )
                }

                path.add(text)
            }

            override fun end(namespace: String?, name: String?) {
                path.removeLast()
            }
        })

        try {
            Xml.parse(inputStream, Xml.Encoding.UTF_8, document.contentHandler)
        } catch (exception: SAXException) {
            throw IOException("OPML parsing failed", exception)
        }
    }

    interface Listener {
        fun onFeedParsed(
                url: String,
                title: String,
                link: String?,
                customTitle: String?,
                category: String?,
                updateMode: UpdateMode
        )
    }

}
