package com.tughi.aggregator.data

import androidx.room.*

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
        @PrimaryKey(autoGenerate = true) val id: Long? = null,
        @ColumnInfo(name = "feed_id") val feedId: Long,

        val uid: String,
        val url: String,
        val title: String,

        val createTime: Long,
        val insertTime: Long,
        val updateTime: Long
)
