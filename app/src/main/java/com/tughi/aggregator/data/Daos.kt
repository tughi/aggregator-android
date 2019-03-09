package com.tughi.aggregator.data

import androidx.lifecycle.LiveData

//@Dao
interface FeedDao {

//    @Insert
    fun insertFeed(feed: Feed): Long

//    @Query("""
//        UPDATE feeds SET
//            url = :url,
//            title = :title,
//            link = :link,
//            language = :language,
//            last_update_error = NULL,
//            last_update_time = :lastUpdateTime,
//            next_update_retry = 0,
//            next_update_time = :nextUpdateTime,
//            http_etag = :httpEtag,
//            http_last_modified = :httpLastModified
//        WHERE id = :id
//    """)
    fun updateFeed(
            id: Long,
            url: String,
            title: String,
            link: String?,
            language: String?,
            lastUpdateTime: Long,
            nextUpdateTime: Long,
            httpEtag: String?,
            httpLastModified: String?

    ): Int

//    @Query("""
//        UPDATE feeds SET
//            url = :url,
//            custom_title = :customTitle,
//            update_mode = :updateMode
//        WHERE id = :id
//    """)
    fun updateFeed(id: Long, url: String, customTitle: String?, updateMode: UpdateMode): Int

//    @Query("""
//        UPDATE feeds SET
//            next_update_time = :nextUpdateTime,
//            next_update_retry = :nextUpdateRetry,
//            last_update_error = :lastUpdateError
//        WHERE id = :id
//    """)
    fun updateFeed(id: Long, nextUpdateTime: Long, nextUpdateRetry: Int = 0, lastUpdateError: String? = null): Int

//    @Query("""
//        UPDATE feeds SET
//            favicon_url = :faviconUrl,
//            favicon_content = :faviconContent
//        WHERE id = :id
//    """)
    fun updateFeed(id: Long, faviconUrl: String, faviconContent: ByteArray): Int

//    @Query("DELETE FROM feeds WHERE id = :feedId")
    fun deleteFeed(feedId: Long): Int

//    @Query("SELECT id FROM feeds WHERE (next_update_time > 0 AND next_update_time < :now) OR next_update_time = -1")
    fun queryOutdatedFeeds(now: Long): LongArray

//    @Query("SELECT id FROM feeds WHERE next_update_time > 0 AND next_update_time < :now")
    fun queryUpdatableFeeds(now: Long): LongArray

//    @Query("SELECT * FROM feeds WHERE id = :id")
    fun queryFeed(id: Long): Feed

//    @Query("SELECT COUNT(1) FROM feeds")
    fun queryFeedCount(): Int

//    @Query("SELECT id, last_update_time, update_mode FROM feeds WHERE id = :feedId")
    fun querySchedulerFeed(feedId: Long): SchedulerFeed?

//    @Query("SELECT id, last_update_time, update_mode FROM feeds WHERE update_mode = :updateMode")
    fun querySchedulerFeeds(updateMode: UpdateMode): Array<SchedulerFeed>

//    @Query("""
//        SELECT
//            url,
//            title,
//            link,
//            custom_title,
//            update_mode,
//            0 AS excluded
//        FROM
//            feeds
//        ORDER BY
//            title
//    """)
    fun queryOpmlFeeds(): List<OpmlFeed>

//    @Query("SELECT * FROM feeds WHERE id = :id")
    fun getFeed(id: Long): LiveData<Feed>

//    @Query("SELECT MIN(next_update_time) FROM feeds WHERE next_update_time > 0")
    fun queryNextUpdateTime(): Long?

}

data class OpmlFeed(
//        @ColumnInfo
        val url: String,

//        @ColumnInfo
        val title: String,

//        @ColumnInfo
        val link: String?,

//        @ColumnInfo(name = "custom_title")
        val customTitle: String?,

//        @ColumnInfo(name = "update_mode")
        val updateMode: UpdateMode,

//        @ColumnInfo
        val excluded: Boolean
)

data class SchedulerFeed(
//        @ColumnInfo
        val id: Long,

//        @ColumnInfo(name = "last_update_time")
        val lastUpdateTime: Long,

//        @ColumnInfo(name = "update_mode")
        val updateMode: UpdateMode
)
