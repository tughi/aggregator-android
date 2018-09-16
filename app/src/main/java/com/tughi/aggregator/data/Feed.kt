package com.tughi.aggregator.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(
        tableName = "feeds"
)
data class Feed(
        @PrimaryKey(autoGenerate = true) val id: Long? = null,
        val url: String,
        val title: String,
        @ColumnInfo(name = "custom_title") val customTitle: String? = null
)