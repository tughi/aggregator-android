package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getBlobOrNull
import androidx.core.database.getStringOrNull
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.StructField
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionBlob
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.tughi.aggregator.data.Feeds

private const val FIELD_ID = "id"
private const val FIELD_URL = "url"
private const val FIELD_TITLE = "title"
private const val FIELD_CUSTOM_TITLE = "customTitle"
private const val FIELD_LINK = "link"
private const val FIELD_LANGUAGE = "language"
private const val FIELD_FAVICON_URL = "faviconUrl"
private const val FIELD_FAVICON_CONTENT = "faviconContent"
private const val FIELD_CLEANUP_MODE = "cleanupMode"
private const val FIELD_UPDATE_MODE = "updateMode"
private const val FIELD_LAST_UPDATE_TIME = "lastUpdateTime"
private const val FIELD_LAST_UPDATE_ERROR = "lastUpdateError"
private const val FIELD_NEXT_UPDATE_TIME = "nextUpdateTime"
private const val FIELD_NEXT_UPDATE_RETRY = "nextUpdateRetry"
private const val FIELD_HTTP_ETAG = "httpEtag"
private const val FIELD_HTTP_LAST_MODIFIED = "httpLastModified"

class Feed private constructor(structElement: StructElement, validate: Boolean) : CustomElement(structElement, validate), Feeds.Insertable {
    constructor(structElement: StructElement) : this(structElement, validate = true)

    constructor(
        id: Long,
        url: String,
        title: String,
        customTitle: String?,
        link: String?,
        language: String?,
        faviconUrl: String?,
        faviconContent: ByteArray?,
        cleanupMode: String,
        updateMode: String,
        lastUpdateTime: Long,
        lastUpdateError: String?,
        nextUpdateTime: Long,
        nextUpdateRetry: Int,
        httpEtag: String?,
        httpLastModified: String?,
    ) : this(
        ionStructOf(
            mutableListOf<StructField>().apply {
                add(field(FIELD_ID, ionInt(id)))
                add(field(FIELD_URL, ionString(url)))
                add(field(FIELD_TITLE, ionString(title)))
                if (customTitle != null) {
                    add(field(FIELD_CUSTOM_TITLE, ionString(customTitle)))
                }
                if (link != null) {
                    add(field(FIELD_LINK, ionString(link)))
                }
                if (language != null) {
                    add(field(FIELD_LANGUAGE, ionString(language)))
                }
                if (faviconUrl != null) {
                    add(field(FIELD_FAVICON_URL, ionString(faviconUrl)))
                }
                if (faviconContent != null) {
                    add(field(FIELD_FAVICON_CONTENT, ionBlob(faviconContent)))
                }
                add(field(FIELD_CLEANUP_MODE, ionString(cleanupMode)))
                add(field(FIELD_UPDATE_MODE, ionString(updateMode)))
                add(field(FIELD_LAST_UPDATE_TIME, ionInt(lastUpdateTime)))
                if (lastUpdateError != null) {
                    add(field(FIELD_LAST_UPDATE_ERROR, ionString(lastUpdateError)))
                }
                add(field(FIELD_NEXT_UPDATE_TIME, ionInt(nextUpdateTime)))
                add(field(FIELD_NEXT_UPDATE_RETRY, ionInt(nextUpdateRetry.toLong())))
                if (httpEtag != null) {
                    add(field(FIELD_HTTP_ETAG, ionString(httpEtag)))
                }
                if (httpLastModified != null) {
                    add(field(FIELD_HTTP_LAST_MODIFIED, ionString(httpLastModified)))
                }
            },
            annotations = listOf(Feed::class.simpleName!!)
        ),
        validate = false,
    )

    override fun validate() {
        checkAnnotation(Feed::class.simpleName!!)
        checkField(FIELD_ID, ElementType.INT)
        checkField(FIELD_URL, ElementType.STRING)
        checkField(FIELD_TITLE, ElementType.STRING)
        checkOptionalField(FIELD_CUSTOM_TITLE, ElementType.STRING)
        checkOptionalField(FIELD_LINK, ElementType.STRING)
        checkOptionalField(FIELD_LANGUAGE, ElementType.STRING)
        checkOptionalField(FIELD_FAVICON_URL, ElementType.STRING)
        checkOptionalField(FIELD_FAVICON_CONTENT, ElementType.BLOB)
        checkField(FIELD_CLEANUP_MODE, ElementType.STRING)
        checkField(FIELD_UPDATE_MODE, ElementType.STRING)
        checkField(FIELD_LAST_UPDATE_TIME, ElementType.INT)
        checkOptionalField(FIELD_LAST_UPDATE_ERROR, ElementType.STRING)
        checkField(FIELD_NEXT_UPDATE_TIME, ElementType.INT)
        checkField(FIELD_NEXT_UPDATE_RETRY, ElementType.INT)
        checkOptionalField(FIELD_HTTP_ETAG, ElementType.STRING)
        checkOptionalField(FIELD_HTTP_LAST_MODIFIED, ElementType.STRING)
    }


    override fun insertData(): Array<Pair<Feeds.TableColumn, Any?>> {
        val columns = mutableListOf<Pair<Feeds.TableColumn, Any?>>()

        for (field in fields) {
            columns.add(
                when (field.name) {
                    FIELD_CLEANUP_MODE -> Feeds.CLEANUP_MODE to field.value.stringValue
                    FIELD_CUSTOM_TITLE -> Feeds.CUSTOM_TITLE to field.value.stringValue
                    FIELD_FAVICON_CONTENT -> Feeds.FAVICON_CONTENT to field.value.bytesValue.copyOfBytes()
                    FIELD_FAVICON_URL -> Feeds.FAVICON_URL to field.value.stringValue
                    FIELD_HTTP_ETAG -> Feeds.HTTP_ETAG to field.value.stringValue
                    FIELD_HTTP_LAST_MODIFIED -> Feeds.HTTP_LAST_MODIFIED to field.value.stringValue
                    FIELD_ID -> Feeds.ID to field.value.longValue
                    FIELD_LANGUAGE -> Feeds.LANGUAGE to field.value.stringValue
                    FIELD_LAST_UPDATE_ERROR -> Feeds.LAST_UPDATE_ERROR to field.value.stringValue
                    FIELD_LAST_UPDATE_TIME -> Feeds.LAST_UPDATE_TIME to field.value.longValue
                    FIELD_LINK -> Feeds.LINK to field.value.stringValue
                    FIELD_NEXT_UPDATE_RETRY -> Feeds.NEXT_UPDATE_RETRY to field.value.longValue.toInt()
                    FIELD_NEXT_UPDATE_TIME -> Feeds.NEXT_UPDATE_TIME to field.value.longValue
                    FIELD_TITLE -> Feeds.TITLE to field.value.stringValue
                    FIELD_UPDATE_MODE -> Feeds.UPDATE_MODE to field.value.stringValue
                    FIELD_URL -> Feeds.URL to field.value.stringValue
                    else -> throw IllegalStateException("Unsupported field: ${field.name}")
                }
            )
        }

        return columns.toTypedArray()
    }

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
}
