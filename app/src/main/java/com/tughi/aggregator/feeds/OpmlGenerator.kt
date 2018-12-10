package com.tughi.aggregator.feeds

import android.util.Xml
import com.tughi.aggregator.data.OpmlFeed
import org.xmlpull.v1.XmlSerializer
import java.io.OutputStream

object OpmlGenerator {

    fun generate(feeds: List<OpmlFeed>, outputStream: OutputStream) {
        val xml = Xml.newSerializer()

        xml.setOutput(outputStream, "utf-8")
        xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

        xml.startTag(null, "opml")
        xml.attribute(null, "version", "2.0")

        xml.startTag(null, "head")
        xml.startTag(null, "title")
        xml.text("Aggregator Feeds")
        xml.endTag(null, "title")
        xml.endTag(null, "head")

        xml.startTag(null, "body")

        feeds.forEach { feed ->
            if (!feed.excluded) {
                xml.startTag(null, "outline")

                xml.attribute(null, "type", "rss")
                writeTitleAttribute(xml, "text", feed.customTitle ?: feed.title)
                writeTitleAttribute(xml, "title", feed.title)
                xml.attribute(null, "xmlUrl", feed.url)
                if (feed.link != null) {
                    xml.attribute(null, "htmlUrl", feed.link)
                }
                xml.attribute(null, "updateMode", feed.updateMode.serialize())

                xml.endTag(null, "outline")
            }
        }

        xml.endTag(null, "body")
        xml.endTag(null, "opml")

        xml.flush()
    }

    private fun writeTitleAttribute(xml: XmlSerializer, attribute: String, title: String) {
        try {
            xml.attribute(null, attribute, title)
        } catch (exception: IllegalArgumentException) {
            val titleLength = title.length

            val newTitle = StringBuilder(titleLength)
            var index = 0
            while (index < titleLength) {
                val codePoint = title.codePointAt(index)
                if (codePoint > Character.MAX_VALUE.toInt()) {
                    newTitle.append("�")
                } else {
                    newTitle.appendCodePoint(codePoint)
                }
                index += Character.charCount(codePoint)
            }

            xml.attribute(null, attribute, newTitle.toString())
        }

    }

}
