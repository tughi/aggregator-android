package com.tughi.aggregator.data

import androidx.room.*

const val DEFAULT_UPDATE_MODE = "DEFAULT"

@Entity(
        tableName = "feeds"
)
data class Feed(
        @PrimaryKey(autoGenerate = true)
        val id: Long? = null,

        @ColumnInfo
        val url: String,

        @ColumnInfo
        val title: String,

        @ColumnInfo(name = "custom_title")
        val customTitle: String? = null,

        @ColumnInfo(name = "update_mode")
        val updateMode: String = DEFAULT_UPDATE_MODE,

        @ColumnInfo(name = "last_successful_update")
        val lastSuccessfulUpdate: Long? = null
)

@Entity(
        tableName = "entries",
        foreignKeys = [
            ForeignKey(
                    entity = Feed::class,
                    parentColumns = ["id"],
                    childColumns = ["feed_id"],
                    onDelete = ForeignKey.CASCADE
            )
        ],
        indices = [
            Index("feed_id", "uid")
        ]
)
data class Entry(
        @PrimaryKey(autoGenerate = true)
        val id: Long? = null,

        @ColumnInfo(name = "feed_id")
        val feedId: Long,

        @ColumnInfo
        val uid: String,

        @ColumnInfo
        val url: String,

        @ColumnInfo
        val title: String,

        @ColumnInfo
        val createTime: Long,

        @ColumnInfo
        val insertTime: Long,

        @ColumnInfo
        val updateTime: Long
)
