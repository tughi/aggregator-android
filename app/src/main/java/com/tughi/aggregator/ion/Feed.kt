package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getBlobOrNull
import androidx.core.database.getStringOrNull
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
}
