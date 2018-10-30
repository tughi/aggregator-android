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

    @Query("SELECT * FROM feeds WHERE id = :id")
    fun getFeed(id: Long): LiveData<Feed>

    @Query("""
        SELECT
            f.id,
            COALESCE(f.custom_title, f.title) AS title,
            f.update_time,
            (SELECT COUNT(1) FROM entries e WHERE f.id = e.feed_id AND e.read_time = 0) AS unread_entry_count,
            0 AS expanded
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

        @ColumnInfo(name = "unread_entry_count")
        val unreadEntryCount: Int,

        @ColumnInfo
        val expanded: Boolean
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
            e.link,
            e.author,
            e.publish_time as formatted_date,
            e.publish_time as formatted_time,
            e.read_time as read_time
        FROM
            entries e
            LEFT JOIN feeds f ON f.id = e.feed_id
        WHERE
            (e.read_time = 0 OR e.read_time > :since)
        ORDER BY
            e.publish_time
    """)
    fun getMyFeedUiEntries(since: Long): DataSource.Factory<Int, UiEntry>

    @Query("""
        SELECT
            e.id,
            COALESCE(f.custom_title, f.title) AS feed_title,
            e.title,
            e.link,
            e.author,
            e.publish_time as formatted_date,
            e.publish_time as formatted_time,
            e.read_time as read_time
        FROM
            entries e
            LEFT JOIN feeds f ON f.id = e.feed_id
        WHERE
            e.feed_id = :feedId AND
            (e.read_time = 0 OR e.read_time > :since)
        ORDER BY
            e.publish_time
    """)
    fun getFeedUiEntries(feedId: Long, since: Long): DataSource.Factory<Int, UiEntry>

    @Query("""
        UPDATE entries SET read_time = :readTime WHERE id = :entryId
    """)
    fun setReadTime(entryId: Long, readTime: Long): Int
}

data class UiEntry(
        @ColumnInfo
        val id: Long,

        @ColumnInfo(name = "feed_title")
        val feedTitle: String,

        @ColumnInfo
        val title: String,

        @ColumnInfo
        val link: String,

        @ColumnInfo
        val author: String?,

        @ColumnInfo(name = "formatted_date")
        val formattedDate: FormattedDate,

        @ColumnInfo(name = "formatted_time")
        val formattedTime: FormattedTime,

        @ColumnInfo(name = "read_time")
        val readTime: Long
)

sealed class UiEntriesGetter {
    abstract fun getUiEntries(entryDao: EntryDao): DataSource.Factory<Int, UiEntry>
}

class FeedUiEntriesGetter(private val feedId: Long, private val since: Long) : UiEntriesGetter() {
    override fun getUiEntries(entryDao: EntryDao): DataSource.Factory<Int, UiEntry> {
        return entryDao.getFeedUiEntries(feedId, since)
    }
}

class MyFeedUiEntriesGetter(private val since: Long) : UiEntriesGetter() {
    override fun getUiEntries(entryDao: EntryDao): DataSource.Factory<Int, UiEntry> {
        return entryDao.getMyFeedUiEntries(since)
    }
}
