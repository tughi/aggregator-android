package com.tughi.aggregator.activities.main

import androidx.room.ColumnInfo
import com.tughi.aggregator.data.FormattedDate
import com.tughi.aggregator.data.FormattedTime
import com.tughi.aggregator.data.UiEntryType

data class EntriesFragmentEntry(
        @ColumnInfo
        val id: Long,

        @ColumnInfo(name = "feed_id")
        val feedId: Long,

        @ColumnInfo(name = "feed_title")
        val feedTitle: String,

        @ColumnInfo(name = "favicon_url")
        val faviconUrl: String?,

        @ColumnInfo
        val title: String?,

        @ColumnInfo
        val link: String?,

        @ColumnInfo
        val author: String?,

        @ColumnInfo(name = "formatted_date")
        val formattedDate: FormattedDate,

        @ColumnInfo(name = "formatted_time")
        val formattedTime: FormattedTime,

        @ColumnInfo(name = "read_time")
        val readTime: Long,

        @ColumnInfo
        val type: UiEntryType
)
