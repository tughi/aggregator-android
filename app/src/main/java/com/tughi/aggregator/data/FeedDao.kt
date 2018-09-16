package com.tughi.aggregator.data

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface FeedDao {

    @Query("SELECT * FROM feeds")
    fun getFeeds(): LiveData<List<Feed>>

    @Insert
    fun addFeed(feed: Feed): Long

}