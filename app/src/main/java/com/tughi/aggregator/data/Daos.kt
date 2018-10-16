package com.tughi.aggregator.data

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
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
            f.id,
            COALESCE(f.custom_title, f.title) AS title,
            f.update_time,
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

        @ColumnInfo(name = "update_time")
        val updateTime: Long,

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

    @Query("""
        SELECT
            e.id,
            COALESCE(f.custom_title, f.title) AS feed_title,
            e.title,
            e.author,
            e.publish_time as formatted_date,
            e.publish_time as formatted_time
        FROM
            entries e
            LEFT JOIN feeds f ON f.id = e.feed_id
        ORDER BY
            e.publish_time
    """)
    fun getUiEntries(): DataSource.Factory<Int, UiEntry>

}

data class UiEntry(
        @ColumnInfo
        val id: Long,

        @ColumnInfo(name = "feed_title")
        val feedTitle: String,

        @ColumnInfo
        val title: String,

        @ColumnInfo
        val author: String?,

        @ColumnInfo(name = "formatted_date")
        val formattedDate: FormattedDate,

        @ColumnInfo(name = "formatted_time")
        val formattedTime: FormattedTime
)
