package com.tughi.aggregator.data

import androidx.room.*

const val UPDATE_MODE__DEFAULT = "DEFAULT"
const val UPDATE_MODE__AUTO = "AUTO"
const val UPDATE_MODE__DISABLED = "DISABLED"

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

        @ColumnInfo
        val link: String? = null,

        @ColumnInfo
        val language: String? = null,

        @ColumnInfo(name = "custom_title")
        val customTitle: String? = null,

        @ColumnInfo(name = "favicon_url")
        val faviconUrl: String? = null,

        @ColumnInfo(name = "favicon_content")
        val faviconContent: ByteArray? = null,

        @ColumnInfo(name = "update_mode")
        val updateMode: String = UPDATE_MODE__DEFAULT,

        @ColumnInfo(name = "last_update_time")
        val lastUpdateTime: Long = 0,

        @ColumnInfo(name = "next_update_retry")
        val nextUpdateRetry: Int = 0,

        @ColumnInfo(name = "next_update_time")
        val nextUpdateTime: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Feed

        if (id != other.id) return false
        if (url != other.url) return false
        if (title != other.title) return false
        if (link != other.link) return false
        if (language != other.language) return false
        if (customTitle != other.customTitle) return false
        if (faviconUrl != other.faviconUrl) return false
        if (updateMode != other.updateMode) return false
        if (lastUpdateTime != other.lastUpdateTime) return false
        if (nextUpdateTime != other.nextUpdateTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + url.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (link?.hashCode() ?: 0)
        result = 31 * result + (language?.hashCode() ?: 0)
        result = 31 * result + (customTitle?.hashCode() ?: 0)
        result = 31 * result + (faviconUrl?.hashCode() ?: 0)
        result = 31 * result + updateMode.hashCode()
        result = 31 * result + lastUpdateTime.hashCode()
        result = 31 * result + nextUpdateTime.hashCode()
        return result
    }
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
        val title: String? = null,

        @ColumnInfo
        val link: String? = null,

        @ColumnInfo
        val content: String? = null,

        @ColumnInfo
        val author: String? = null,

        @ColumnInfo(name = "publish_time")
        val publishTime: Long? = null,

        @ColumnInfo(name = "insert_time")
        val insertTime: Long,

        @ColumnInfo(name = "update_time")
        val updateTime: Long,

        @ColumnInfo(name = "read_time")
        val readTime: Long = 0
)
