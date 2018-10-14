package com.tughi.aggregator.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FeedDao {

    @Insert
    fun insertFeed(feed: Feed): Long

    @Update
    fun updateFeed(feed: Feed): Int

    @Query("SELECT id FROM feeds")
    fun queryFeedIds(): LongArray

    @Query("SELECT * FROM feeds WHERE id = :id")
    fun queryFeed(id: Long): Feed

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
    fun insertEntry(entry: Entry): Long

    @Update
    fun updateEntry(entry: Entry): Int

    @Query("SELECT * FROM entries WHERE feed_id = :feedId AND uid = :uid")
    fun queryEntry(feedId: Long, uid: String): Entry?

}
