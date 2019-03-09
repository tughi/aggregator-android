package com.tughi.aggregator.data

//@Dao
interface FeedDao {

//    @Insert
    fun insertFeed(feed: Feed): Long

//    @Query("""
//        UPDATE feeds SET
//            next_update_time = :nextUpdateTime,
//            next_update_retry = :nextUpdateRetry,
//            last_update_error = :lastUpdateError
//        WHERE id = :id
//    """)
    fun updateFeed(id: Long, nextUpdateTime: Long, nextUpdateRetry: Int = 0, lastUpdateError: String? = null): Int

//    @Query("SELECT id FROM feeds WHERE next_update_time > 0 AND next_update_time < :now")
    fun queryUpdatableFeeds(now: Long): LongArray

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
