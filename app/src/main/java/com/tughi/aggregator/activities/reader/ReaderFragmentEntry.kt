package com.tughi.aggregator.activities.reader

import androidx.room.ColumnInfo

data class ReaderFragmentEntry(
        @ColumnInfo
        val id: Long,

        @ColumnInfo
        val title: String?,

        @ColumnInfo
        val link: String?,

        @ColumnInfo
        val content: String?,

        @ColumnInfo
        val author: String?,

        @ColumnInfo(name = "publish_time")
        val publishTime: Long,

        @ColumnInfo(name = "feed_title")
        val feedTitle: String,

        @ColumnInfo(name = "feed_language")
        val feedLanguage: String?,

        @ColumnInfo(name = "read_time")
        val readTime: Long,

        @ColumnInfo(name = "pinned_time")
        val pinnedTime: Long
)
