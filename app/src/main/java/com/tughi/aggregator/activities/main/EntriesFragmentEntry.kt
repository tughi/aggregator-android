package com.tughi.aggregator.activities.main

data class EntriesFragmentEntry(
//        @ColumnInfo
        val id: Long,

//        @ColumnInfo(name = "feed_id")
        val feedId: Long,

//        @ColumnInfo(name = "feed_title")
        val feedTitle: String,

//        @ColumnInfo(name = "favicon_url")
        val faviconUrl: String?,

//        @ColumnInfo
        val title: String?,

//        @ColumnInfo
        val link: String?,

//        @ColumnInfo
        val author: String?,

//        @ColumnInfo(name = "formatted_date")
        val formattedDate: String,

//        @ColumnInfo(name = "formatted_time")
        val formattedTime: String,

//        @ColumnInfo(name = "read_time")
        val readTime: Long,

//        @ColumnInfo(name = "pinned_time")
        val pinnedTime: Long,

//        @ColumnInfo
        val type: EntriesFragmentEntryType
)
