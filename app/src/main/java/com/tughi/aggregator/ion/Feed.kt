package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getBlobOrNull
import androidx.core.database.getStringOrNull
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonWriter
import com.tughi.aggregator.data.Feeds

class Feed(
    val id: Long,
    val url: String,
    val title: String,
    val customTitle: String?,
    val link: String?,
    val language: String?,
    val faviconUrl: String?,
    val faviconContent: ByteArray?,
    val cleanupMode: String,
    val updateMode: String,
    val lastUpdateTime: Long,
    val lastUpdateError: String?,
    val nextUpdateTime: Long,
    val nextUpdateRetry: Int,
    val httpEtag: String?,
    val httpLastModified: String?,
) {
    object QueryHelper : Feeds.QueryHelper<Feed>(
        Feeds.ID,
        Feeds.URL,
        Feeds.TITLE,
        Feeds.CUSTOM_TITLE,
        Feeds.LINK,
        Feeds.LANGUAGE,
        Feeds.FAVICON_URL,
        Feeds.FAVICON_CONTENT,
        Feeds.CLEANUP_MODE,
        Feeds.UPDATE_MODE,
        Feeds.LAST_UPDATE_TIME,
        Feeds.LAST_UPDATE_ERROR,
        Feeds.NEXT_UPDATE_TIME,
        Feeds.NEXT_UPDATE_RETRY,
        Feeds.HTTP_ETAG,
        Feeds.HTTP_LAST_MODIFIED,
    ) {
        override fun createRow(cursor: Cursor) = Feed(
            id = cursor.getLong(0),
            url = cursor.getString(1),
            title = cursor.getString(2),
            customTitle = cursor.getStringOrNull(3),
            link = cursor.getStringOrNull(4),
            language = cursor.getStringOrNull(5),
            faviconUrl = cursor.getStringOrNull(6),
            faviconContent = cursor.getBlobOrNull(7),
            cleanupMode = cursor.getString(8),
            updateMode = cursor.getString(9),
            lastUpdateTime = cursor.getLong(10),
            lastUpdateError = cursor.getStringOrNull(11),
            nextUpdateTime = cursor.getLong(12),
            nextUpdateRetry = cursor.getInt(13),
            httpEtag = cursor.getStringOrNull(14),
            httpLastModified = cursor.getStringOrNull(15),
        )
    }

    fun writeTo(ionWriter: IonWriter, ionSystem: IonSystem) {
        ionSystem.newEmptyStruct().apply {
            setTypeAnnotations(Feed::class.simpleName)
            add(Feed::id.name, ionSystem.newInt(id))
            add(Feed::url.name, ionSystem.newString(url))
            add(Feed::title.name, ionSystem.newString(title))
            if (customTitle != null) {
                add(Feed::customTitle.name, ionSystem.newString(customTitle))
            }
            if (link != null) {
                add(Feed::link.name, ionSystem.newString(link))
            }
            if (language != null) {
                add(Feed::language.name, ionSystem.newString(language))
            }
            if (faviconUrl != null) {
                add(Feed::faviconUrl.name, ionSystem.newString(faviconUrl))
            }
            if (faviconContent != null) {
                add(Feed::faviconContent.name, ionSystem.newBlob(faviconContent))
            }
            add(Feed::cleanupMode.name, ionSystem.newString(cleanupMode))
            add(Feed::updateMode.name, ionSystem.newString(updateMode))
            add(Feed::lastUpdateTime.name, ionSystem.newInt(lastUpdateTime))
            if (lastUpdateError != null) {
                add(Feed::lastUpdateError.name, ionSystem.newString(lastUpdateError))
            }
            add(Feed::nextUpdateTime.name, ionSystem.newInt(nextUpdateTime))
            add(Feed::nextUpdateRetry.name, ionSystem.newInt(nextUpdateRetry))
            if (httpEtag != null) {
                add(Feed::httpEtag.name, ionSystem.newString(httpEtag))
            }
            if (httpLastModified != null) {
                add(Feed::httpLastModified.name, ionSystem.newString(httpLastModified))
            }
        }.writeTo(ionWriter)
    }
}

fun feedData(ionStruct: IonStruct): Array<Pair<Feeds.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<Feeds.TableColumn, Any?>>()

    for (ionValue in ionStruct) {
        columns.add(
            when (ionValue.fieldName) {
                Feed::cleanupMode.name -> Feeds.CLEANUP_MODE to ionValue.stringValue()
                Feed::customTitle.name -> Feeds.CUSTOM_TITLE to ionValue.stringValue()
                Feed::httpEtag.name -> Feeds.HTTP_ETAG to ionValue.stringValue()
                Feed::httpLastModified.name -> Feeds.HTTP_LAST_MODIFIED to ionValue.stringValue()
                Feed::id.name -> Feeds.ID to ionValue.longValue()
                Feed::language.name -> Feeds.LANGUAGE to ionValue.stringValue()
                Feed::faviconUrl.name -> Feeds.FAVICON_URL to ionValue.stringValue()
                Feed::faviconContent.name -> Feeds.FAVICON_CONTENT to ionValue.bytesValue()
                Feed::lastUpdateError.name -> Feeds.LAST_UPDATE_ERROR to ionValue.stringValue()
                Feed::lastUpdateTime.name -> Feeds.LAST_UPDATE_TIME to ionValue.longValue()
                Feed::link.name -> Feeds.LINK to ionValue.stringValue()
                Feed::nextUpdateRetry.name -> Feeds.NEXT_UPDATE_RETRY to ionValue.intValue()
                Feed::nextUpdateTime.name -> Feeds.NEXT_UPDATE_TIME to ionValue.longValue()
                Feed::title.name -> Feeds.TITLE to ionValue.stringValue()
                Feed::updateMode.name -> Feeds.UPDATE_MODE to ionValue.stringValue()
                Feed::url.name -> Feeds.URL to ionValue.stringValue()
                else -> throw IllegalStateException("Unsupported field: ${ionValue.fieldName}")
            }
        )
    }

    return columns.toTypedArray()
}
