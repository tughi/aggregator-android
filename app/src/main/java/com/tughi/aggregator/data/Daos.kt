package com.tughi.aggregator.data

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.io.Serializable

@Dao
interface FeedDao {

    @Insert
    fun insertFeed(feed: Feed): Long

    @Query("""
        UPDATE feeds SET
            url = :url,
            title = :title,
            link = :link,
            language = :language,
            last_update_time = :lastUpdateTime,
            next_update_retry = 0,
            next_update_time = :nextUpdateTime
        WHERE id = :id
    """)
    fun updateFeed(id: Long, url: String, title: String, link: String?, language: String?, lastUpdateTime: Long, nextUpdateTime: Long): Int

    @Query("""
        UPDATE feeds SET
            url = :url,
            custom_title = :customTitle,
            update_mode = :updateMode
        WHERE id = :id
    """)
    fun updateFeed(id: Long, url: String, customTitle: String?, updateMode: UpdateMode): Int

    @Query("""
        UPDATE feeds SET
            next_update_time = :nextUpdateTime,
            next_update_retry = :nextUpdateRetry,
            last_update_error = :lastUpdateError
        WHERE id = :id
    """)
    fun updateFeed(id: Long, nextUpdateTime: Long, nextUpdateRetry: Int = 0, lastUpdateError: String? = null): Int

    @Query("""
        UPDATE feeds SET
            favicon_url = :faviconUrl,
            favicon_content = :faviconContent
        WHERE id = :id
    """)
    fun updateFeed(id: Long, faviconUrl: String, faviconContent: ByteArray): Int

    @Query("DELETE FROM feeds WHERE id = :feedId")
    fun deleteFeed(feedId: Long): Int

    @Query("SELECT id FROM feeds WHERE (next_update_time > 0 AND next_update_time < :now) OR next_update_time = -1")
    fun queryOutdatedFeeds(now: Long): LongArray

    @Query("SELECT id FROM feeds WHERE next_update_time > 0 AND next_update_time < :now")
    fun queryUpdatableFeeds(now: Long): LongArray

    @Query("SELECT * FROM feeds WHERE id = :id")
    fun queryFeed(id: Long): Feed

    @Query("SELECT COUNT(1) FROM feeds")
    fun queryFeedCount(): Int

    @Query("SELECT id, last_update_time, update_mode FROM feeds WHERE id = :feedId")
    fun querySchedulerFeed(feedId: Long): SchedulerFeed?

    @Query("SELECT id, last_update_time, update_mode FROM feeds WHERE update_mode = :updateMode")
    fun querySchedulerFeeds(updateMode: UpdateMode): Array<SchedulerFeed>

    @Query("""
        SELECT
            url,
            title,
            link,
            custom_title,
            update_mode,
            0 AS excluded
        FROM
            feeds
        ORDER BY
            title
    """)
    fun queryOpmlFeeds(): List<OpmlFeed>

    @Query("SELECT * FROM feeds WHERE id = :id")
    fun getFeed(id: Long): LiveData<Feed>

    @Query("""
        SELECT
            f.id,
            COALESCE(f.custom_title, f.title) AS title,
            f.favicon_url,
            f.last_update_time,
            f.last_update_error,
            f.next_update_time,
            f.next_update_retry,
            f.update_mode,
            (SELECT COUNT(1) FROM entries e WHERE f.id = e.feed_id AND e.read_time = 0) AS unread_entry_count,
            0 AS expanded,
            0 AS updating
        FROM
            feeds f
        ORDER BY
            title
    """)
    fun getUiFeeds(): LiveData<List<UiFeed>>

    @Query("SELECT MIN(next_update_time) FROM feeds WHERE next_update_time > 0")
    fun queryNextUpdateTime(): Long?

}

data class OpmlFeed(
        @ColumnInfo
        val url: String,

        @ColumnInfo
        val title: String,

        @ColumnInfo
        val link: String?,

        @ColumnInfo(name = "custom_title")
        val customTitle: String?,

        @ColumnInfo(name = "update_mode")
        val updateMode: UpdateMode,

        @ColumnInfo
        val excluded: Boolean
)

data class SchedulerFeed(
        @ColumnInfo
        val id: Long,

        @ColumnInfo(name = "last_update_time")
        val lastUpdateTime: Long,

        @ColumnInfo(name = "update_mode")
        val updateMode: UpdateMode
)

data class UiFeed(
        @ColumnInfo
        val id: Long,

        @ColumnInfo
        val title: String,

        @ColumnInfo(name = "favicon_url")
        val faviconUrl: String?,

        @ColumnInfo(name = "last_update_time")
        val lastUpdateTime: Long,

        @ColumnInfo(name = "last_update_error")
        val lastUpdateError: String?,

        @ColumnInfo(name = "next_update_time")
        val nextUpdateTime: Long,

        @ColumnInfo(name = "next_update_retry")
        val nextUpdateRetry: Int,

        @ColumnInfo(name = "update_mode")
        val updateMode: UpdateMode,

        @ColumnInfo(name = "unread_entry_count")
        val unreadEntryCount: Int,

        @ColumnInfo
        val expanded: Boolean,

        @ColumnInfo
        val updating: Boolean
) : Serializable

@Dao
interface EntryDao {

    @Insert
    fun insertEntry(entry: Entry): Long

    @Query("""
        UPDATE entries SET
            title = :title,
            link = :link,
            content = :content,
            author = :author,
            publish_time = :publishTime,
            update_time = :updateTime
        WHERE id = :id
    """)
    fun updateEntry(id: Long, title: String?, link: String?, content: String?, author: String?, publishTime: Long?, updateTime: Long): Int

    @Query("SELECT * FROM entries WHERE feed_id = :feedId AND uid = :uid")
    fun queryEntry(feedId: Long, uid: String): Entry?

    @Query("""
        SELECT
            e.id,
            f.id AS feed_id,
            COALESCE(f.custom_title, f.title) AS feed_title,
            f.favicon_url,
            e.title,
            e.link,
            e.author,
            COALESCE(e.publish_time, e.insert_time) AS formatted_date,
            COALESCE(e.publish_time, e.insert_time) AS formatted_time,
            e.read_time AS read_time,
            e.read_time > 0 AS type
        FROM
            entries e
            LEFT JOIN feeds f ON f.id = e.feed_id
        WHERE
            (e.read_time = 0 OR e.read_time > :since)
        ORDER BY
            COALESCE(e.publish_time, e.insert_time)
    """)
    fun getMyFeedUiEntries(since: Long): LiveData<Array<UiEntry>>

    @Query("""
        SELECT
            e.id,
            f.id AS feed_id,
            COALESCE(f.custom_title, f.title) AS feed_title,
            f.favicon_url,
            e.title,
            e.link,
            e.author,
            COALESCE(e.publish_time, e.insert_time) AS formatted_date,
            COALESCE(e.publish_time, e.insert_time) AS formatted_time,
            e.read_time AS read_time,
            e.read_time > 0 AS type
        FROM
            entries e
            LEFT JOIN feeds f ON f.id = e.feed_id
        WHERE
            e.feed_id = :feedId AND
            (e.read_time = 0 OR e.read_time > :since)
        ORDER BY
            COALESCE(e.publish_time, e.insert_time)
    """)
    fun getFeedUiEntries(feedId: Long, since: Long): LiveData<Array<UiEntry>>

    @Query("""
        UPDATE entries SET read_time = :readTime WHERE id = :entryId
    """)
    fun setReadTime(entryId: Long, readTime: Long): Int

    @Query("SELECT COUNT(1) FROM entries WHERE feed_id = :feedId AND COALESCE(publish_time, insert_time) > :since")
    fun countAggregatedEntries(feedId: Long, since: Long): Int

}

data class UiEntry(
        @ColumnInfo
        val id: Long,

        @ColumnInfo(name = "feed_id")
        val feedId: Long,

        @ColumnInfo(name = "feed_title")
        val feedTitle: String,

        @ColumnInfo(name = "favicon_url")
        val faviconUrl: String?,

        @ColumnInfo
        val title: String?,

        @ColumnInfo
        val link: String?,

        @ColumnInfo
        val author: String?,

        @ColumnInfo(name = "formatted_date")
        val formattedDate: FormattedDate,

        @ColumnInfo(name = "formatted_time")
        val formattedTime: FormattedTime,

        @ColumnInfo(name = "read_time")
        val readTime: Long,

        @ColumnInfo
        val type: UiEntryType
)

sealed class UiEntriesGetter {
    abstract fun getUiEntries(entryDao: EntryDao): LiveData<Array<UiEntry>>
}

class FeedUiEntriesGetter(private val feedId: Long, private val since: Long) : UiEntriesGetter() {
    override fun getUiEntries(entryDao: EntryDao): LiveData<Array<UiEntry>> {
        return entryDao.getFeedUiEntries(feedId, since)
    }
}

class MyFeedUiEntriesGetter(private val since: Long) : UiEntriesGetter() {
    override fun getUiEntries(entryDao: EntryDao): LiveData<Array<UiEntry>> {
        return entryDao.getMyFeedUiEntries(since)
    }
}
