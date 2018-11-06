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
        val updateMode: String = DEFAULT_UPDATE_MODE,

        @ColumnInfo(name = "update_time")
        val updateTime: Long = 0
) {
    fun updated(
            url: String,
            title: String,
            link: String?,
            language: String?,
            updateTime: Long
    ) = Feed(
            id = this.id,
            url = url,
            title = title,
            link = link,
            language = language,
            customTitle = this.customTitle,
            updateMode = this.updateMode,
            updateTime = updateTime
    )

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
        if (updateTime != other.updateTime) return false

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
        result = 31 * result + updateTime.hashCode()
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

        @ColumnInfo(name = "insert_time")
        val insertTime: Long,

        @ColumnInfo(name = "publish_time")
        val publishTime: Long,

        @ColumnInfo(name = "update_time")
        val updateTime: Long,

        @ColumnInfo(name = "read_time")
        val readTime: Long = 0
) {
    fun updated(
            title: String?,
            link: String?,
            content: String?,
            author: String?,
            publishTime: Long?,
            updateTime: Long
    ) = Entry(
            id = this.id,
            feedId = this.feedId,
            uid = this.uid,
            title = title,
            link = link,
            content = content,
            author = author,
            insertTime = this.insertTime,
            publishTime = publishTime ?: this.publishTime,
            updateTime = updateTime
    )
}
