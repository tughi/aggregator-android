package com.tughi.aggregator.activities.main

import androidx.room.ColumnInfo
import com.tughi.aggregator.data.UpdateMode
import java.io.Serializable

data class FeedsFragmentFeed(
        @ColumnInfo
        val id: Long,

        @ColumnInfo
        val title: String,

        @ColumnInfo(name = "favicon_url")
        val faviconUrl: String?,

        @ColumnInfo(name = "last_update_time")
        val lastUpdateTime: Long,

        @ColumnInfo(name = "last_update_error")
        val lastUpdateError: String?,

        @ColumnInfo(name = "next_update_time")
        val nextUpdateTime: Long,

        @ColumnInfo(name = "next_update_retry")
        val nextUpdateRetry: Int,

        @ColumnInfo(name = "update_mode")
        val updateMode: UpdateMode,

        @ColumnInfo(name = "unread_entry_count")
        val unreadEntryCount: Int,

        @ColumnInfo
        val expanded: Boolean,

        @ColumnInfo
        val updating: Boolean
) : Serializable
