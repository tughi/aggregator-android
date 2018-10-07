package com.tughi.aggregator.data

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FeedDao {

    @Insert
    fun addFeed(feed: Feed): Long

    @Query("""
        SELECT
            f.id AS id,
            COALESCE(f.custom_title, f.title) AS title,
            (SELECT COUNT(1) FROM entries e WHERE f.id = e.feed_id) AS entry_count
        FROM
            feeds f
        ORDER BY
            title
    """)
    fun getUiFeeds(): LiveData<List<UiFeed>>

}

data class UiFeed(
        @ColumnInfo
        val id: Long,

        @ColumnInfo
        val title: String,

        @ColumnInfo(name = "entry_count")
        val entryCount: Int
)

@Dao
interface EntryDao {

    @Insert
    fun addEntry(entry: Entry): Long

}
