package com.tughi.aggregator.feeds

import android.database.Cursor
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.UpdateMode

data class OpmlFeed(
        val url: String,
        val title: String,
        val link: String?,
        val customTitle: String?,
        val category: String? = null,
        val updateMode: UpdateMode,
        val cleanupMode: CleanupMode,
        val excluded: Boolean = false
) {
    object QueryHelper : Feeds.QueryHelper<OpmlFeed>(
            Feeds.URL,
            Feeds.TITLE,
            Feeds.CUSTOM_TITLE,
            Feeds.LINK,
            Feeds.UPDATE_MODE,
            Feeds.CLEANUP_MODE
    ) {
        override fun createRow(cursor: Cursor) = OpmlFeed(
                url = cursor.getString(0),
                title = cursor.getString(1),
                customTitle = cursor.getString(2),
                link = cursor.getString(3),
                updateMode = UpdateMode.deserialize(cursor.getString(4)),
                cleanupMode = CleanupMode.deserialize(cursor.getString(5))
        )
    }
}
