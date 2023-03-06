package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.tughi.aggregator.data.Entries

class Entry(
    val id: Long,
    val uid: String,
    val feedId: Long,
    val title: String?,
    val link: String?,
    val content: String?,
    val author: String?,
    val publishTime: Long?,
    val insertTime: Long,
    val updateTime: Long,
    val readTime: Long,
    val pinnedTime: Long,
    val starredTime: Long,
) {
    object QueryHelper : Entries.QueryHelper<Entry>(
        Entries.ID,
        Entries.UID,
        Entries.FEED_ID,
        Entries.TITLE,
        Entries.LINK,
        Entries.CONTENT,
        Entries.AUTHOR,
        Entries.PUBLISH_TIME,
        Entries.INSERT_TIME,
        Entries.UPDATE_TIME,
        Entries.READ_TIME,
        Entries.PINNED_TIME,
        Entries.STARRED_TIME,
    ) {
        override fun createRow(cursor: Cursor) = Entry(
            id = cursor.getLong(0),
            uid = cursor.getString(1),
            feedId = cursor.getLong(2),
            title = cursor.getStringOrNull(3),
            link = cursor.getStringOrNull(4),
            content = cursor.getStringOrNull(5),
            author = cursor.getStringOrNull(6),
            publishTime = cursor.getLongOrNull(7),
            insertTime = cursor.getLong(8),
            updateTime = cursor.getLong(9),
            readTime = cursor.getLong(10),
            pinnedTime = cursor.getLong(11),
            starredTime = cursor.getLong(12),
        )
    }
}
