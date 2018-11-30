package com.tughi.aggregator.services

import android.text.format.DateUtils
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.UpdateSettings
import com.tughi.aggregator.data.AutoUpdateMode
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.DisabledUpdateMode
import com.tughi.aggregator.data.RepeatingUpdateMode
import com.tughi.aggregator.data.SchedulerFeed
import com.tughi.aggregator.data.UpdateMode
import kotlin.math.max
import kotlin.math.min

object FeedUpdaterScheduler {

    fun scheduleFeed(feedId: Long) {
        val database = AppDatabase.instance
        val feedDao = database.feedDao()

        feedDao.querySchedulerFeed(feedId)?.also {
            scheduleFeeds(it)
        }
    }

    fun scheduleFeedsWithDefaultUpdateMode() {
        val database = AppDatabase.instance
        val feedDao = database.feedDao()

        scheduleFeeds(*feedDao.querySchedulerFeeds(DefaultUpdateMode))
    }

    private fun scheduleFeeds(vararg feeds: SchedulerFeed) {
        val database = AppDatabase.instance
        val feedDao = database.feedDao()

        database.beginTransaction()
        try {
            feeds.forEach { feed ->
                feedDao.updateFeed(feed.id, calculateNextUpdateTime(feed.id, feed.updateMode, feed.lastUpdateTime))
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        // TODO: schedule job service
    }

    fun calculateNextUpdateTime(feedId: Long, updateMode: UpdateMode, lastUpdateTime: Long): Long = when (updateMode) {
        AutoUpdateMode -> calculateNextAutoUpdateTime(feedId, lastUpdateTime)
        DefaultUpdateMode -> calculateNextUpdateTime(feedId, UpdateSettings.defaultUpdateMode, lastUpdateTime)
        DisabledUpdateMode -> 0
        is RepeatingUpdateMode -> throw IllegalArgumentException("Unsupported update mode: $updateMode")
    }

    private fun calculateNextAutoUpdateTime(feedId: Long, lastUpdateTime: Long): Long {
        val entryDao = AppDatabase.instance.entryDao()

        val aggregatedEntriesSinceYesterday = entryDao.countAggregatedEntries(feedId, lastUpdateTime - DateUtils.DAY_IN_MILLIS)
        val updateRate = if (aggregatedEntriesSinceYesterday > 0) {
            max(DateUtils.DAY_IN_MILLIS / aggregatedEntriesSinceYesterday, DateUtils.HOUR_IN_MILLIS / 2) / 2
        } else {
            val aggregatedEntriesSinceLastWeek = entryDao.countAggregatedEntries(feedId, lastUpdateTime - DateUtils.WEEK_IN_MILLIS)
            if (aggregatedEntriesSinceLastWeek > 0) {
                min(DateUtils.WEEK_IN_MILLIS / aggregatedEntriesSinceLastWeek, DateUtils.DAY_IN_MILLIS / 2) / 2
            } else {
                DateUtils.DAY_IN_MILLIS / 2
            }
        }
        val alignedUpdateRate = updateRate / (DateUtils.HOUR_IN_MILLIS / 4) * (DateUtils.HOUR_IN_MILLIS / 4)
        return lastUpdateTime / (DateUtils.HOUR_IN_MILLIS / 4) * (DateUtils.HOUR_IN_MILLIS / 4) + alignedUpdateRate
    }

}
