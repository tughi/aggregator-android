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

}
