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
        val link: String? = null,

        @ColumnInfo
        val title: String,

        @ColumnInfo
        val language: String? = null,

        @ColumnInfo(name = "custom_title")
        val customTitle: String? = null,

        @ColumnInfo(name = "update_mode")
        val updateMode: String = DEFAULT_UPDATE_MODE,

        @ColumnInfo(name = "last_successful_update")
        val lastSuccessfulUpdate: Long? = null
) {
    fun updated(
            url: String,
            link: String?,
            title: String,
            language: String?,
            lastSuccessfulUpdate: Long
    ) = Feed(
            id = this.id,
            url = url,
            link = link,
            title = title,
            language = language,
            customTitle = this.customTitle,
            updateMode = this.updateMode,
            lastSuccessfulUpdate = lastSuccessfulUpdate
    )
}

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
            Index("feed_id", "uid", unique = true)
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
        val link: String? = null,

        @ColumnInfo
        val title: String? = null,

        @ColumnInfo
        val content: String? = null,

        @ColumnInfo
        val author: String? = null,

        @ColumnInfo(name = "insert_time")
        val insertTime: Long,

        @ColumnInfo(name = "publish_time")
        val publishTime: Long,

        @ColumnInfo(name = "update_time")
        val updateTime: Long
) {
    fun updated(
            link: String?,
            title: String?,
            content: String?,
            author: String?,
            publishTime: Long?,
            updateTime: Long
    ) = Entry(
            id = this.id,
            feedId = this.feedId,
            uid = this.uid,
            link = link,
            title = title,
            content = content,
            author = author,
            insertTime = this.insertTime,
            publishTime = publishTime ?: this.publishTime,
            updateTime = updateTime
    )
}
