package com.tughi.aggregator.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
        tableName = "feeds"
)
data class Feed(
        @PrimaryKey(autoGenerate = true) val id: Long? = null,
        val url: String,
        val title: String,
        @ColumnInfo(name = "custom_title") val customTitle: String? = null
)