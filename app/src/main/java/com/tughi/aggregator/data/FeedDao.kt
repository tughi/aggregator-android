package com.tughi.aggregator.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FeedDao {

    @Query("SELECT * FROM feeds")
    fun getFeeds(): LiveData<List<Feed>>

    @Insert
    fun addFeed(feed: Feed): Long

}