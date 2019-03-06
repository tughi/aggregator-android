package com.tughi.aggregator.activities.main

import androidx.lifecycle.LiveData

abstract class MainDao {

//    @Query("""
//        SELECT
//            f.id,
//            COALESCE(f.custom_title, f.title) AS title,
//            f.favicon_url,
//            f.last_update_time,
//            f.last_update_error,
//            f.next_update_time,
//            f.next_update_retry,
//            f.update_mode,
//            (SELECT COUNT(1) FROM entries e WHERE f.id = e.feed_id AND e.read_time = 0) AS unread_entry_count,
//            0 AS expanded,
//            0 AS updating
//        FROM
//            feeds f
//        ORDER BY
//            title
//    """)
    abstract fun getFeedsFragmentFeeds(): LiveData<List<FeedsFragmentFeed>>

}
