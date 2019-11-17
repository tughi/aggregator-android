package com.tughi.aggregator.services

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.database.Cursor
import android.text.format.DateUtils
import android.util.Log
import com.tughi.aggregator.App
import com.tughi.aggregator.JOB_SERVICE_FEEDS_UPDATER
import com.tughi.aggregator.data.AdaptiveUpdateMode
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.DisabledUpdateMode
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.Every15MinutesUpdateMode
import com.tughi.aggregator.data.Every2HoursUpdateMode
import com.tughi.aggregator.data.Every30MinutesUpdateMode
import com.tughi.aggregator.data.Every3HoursUpdateMode
import com.tughi.aggregator.data.Every45MinutesUpdateMode
import com.tughi.aggregator.data.Every4HoursUpdateMode
import com.tughi.aggregator.data.Every6HoursUpdateMode
import com.tughi.aggregator.data.Every8HoursUpdateMode
import com.tughi.aggregator.data.EveryHourUpdateMode
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.OnAppLaunchUpdateMode
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.preferences.UpdateSettings
import java.util.Calendar
import java.util.Date
import kotlin.math.max
import kotlin.math.min

object AutoUpdateScheduler {

    const val NEXT_UPDATE_TIME__DISABLED = 0L
    const val NEXT_UPDATE_TIME__ON_APP_LAUNCH = -1L

    fun scheduleFeed(feedId: Long) {
        Feeds.queryOne(Feeds.QueryRowCriteria(feedId), Feed.QueryHelper)?.also {
            scheduleFeeds(it)
        }
    }

    fun scheduleFeedsWithDefaultUpdateMode() {
        val feeds = Feeds.query(Feeds.UpdateModeCriteria(DefaultUpdateMode), Feed.QueryHelper)
        scheduleFeeds(*feeds.toTypedArray())
    }

    private fun scheduleFeeds(vararg feeds: Feed) {
        Database.transaction {
            feeds.forEach { feed ->
                Feeds.update(
                        Feeds.UpdateRowCriteria(feed.id),
                        Feeds.NEXT_UPDATE_TIME to calculateNextUpdateTime(feed.id, feed.updateMode, feed.lastUpdateTime)
                )
            }
        }

        schedule()
    }

    fun schedule() {
        val nextUpdateTime = Feeds.queryFirstNextUpdateTime() ?: return
        val nextUpdateDelay = nextUpdateTime - System.currentTimeMillis()

        val context: Context = App.instance
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        val jobInfo = JobInfo.Builder(JOB_SERVICE_FEEDS_UPDATER, ComponentName(context, AutoUpdateService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(max(0, nextUpdateDelay))
                .setPersisted(true)
                .build()

        Log.d(javaClass.name, "Schedule next auto update: ${Date(nextUpdateTime)}")

        jobScheduler.schedule(jobInfo)
    }

    fun cancel() {
        val jobScheduler = App.instance.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(JOB_SERVICE_FEEDS_UPDATER)
    }

    fun calculateNextUpdateRetryTime(updateMode: UpdateMode, retry: Int): Long = when (updateMode) {
        DefaultUpdateMode -> calculateNextUpdateRetryTime(UpdateSettings.defaultUpdateMode, retry)
        DisabledUpdateMode -> NEXT_UPDATE_TIME__DISABLED
        OnAppLaunchUpdateMode -> NEXT_UPDATE_TIME__ON_APP_LAUNCH
        else -> calculateNextUpdateRetryTime(retry)
    }

    private fun calculateNextUpdateRetryTime(retry: Int): Long {
        val fifteenMinutes = 15 * DateUtils.MINUTE_IN_MILLIS
        val currentTimeSlot = (System.currentTimeMillis() / fifteenMinutes) * fifteenMinutes

        return currentTimeSlot + when (retry) {
            1, 2, 3 -> retry * fifteenMinutes
            4, 5, 6 -> (retry - 3) * DateUtils.HOUR_IN_MILLIS
            7, 8, 9 -> (retry - 6) * 4 * DateUtils.HOUR_IN_MILLIS
            10, 11, 12 -> (retry - 9) * DateUtils.DAY_IN_MILLIS
            else -> DateUtils.WEEK_IN_MILLIS
        }
    }

    fun calculateNextUpdateTime(feedId: Long, updateMode: UpdateMode, lastUpdateTime: Long): Long = when (updateMode) {
        AdaptiveUpdateMode -> calculateNextAdaptiveUpdateTime(feedId, lastUpdateTime)
        DefaultUpdateMode -> calculateNextUpdateTime(feedId, UpdateSettings.defaultUpdateMode, lastUpdateTime)
        DisabledUpdateMode -> NEXT_UPDATE_TIME__DISABLED
        OnAppLaunchUpdateMode -> NEXT_UPDATE_TIME__ON_APP_LAUNCH
        Every15MinutesUpdateMode -> calculateNextRepeatingUpdateTime(lastUpdateTime, 15)
        Every30MinutesUpdateMode -> calculateNextRepeatingUpdateTime(lastUpdateTime, 30)
        Every45MinutesUpdateMode -> calculateNextRepeatingUpdateTime(lastUpdateTime, 45)
        EveryHourUpdateMode -> calculateNextRepeatingUpdateTime(lastUpdateTime, 60)
        Every2HoursUpdateMode -> calculateNextRepeatingUpdateTime(lastUpdateTime, 120)
        Every3HoursUpdateMode -> calculateNextRepeatingUpdateTime(lastUpdateTime, 180)
        Every4HoursUpdateMode -> calculateNextRepeatingUpdateTime(lastUpdateTime, 240)
        Every6HoursUpdateMode -> calculateNextRepeatingUpdateTime(lastUpdateTime, 360)
        Every8HoursUpdateMode -> calculateNextRepeatingUpdateTime(lastUpdateTime, 480)
    }

    private fun calculateNextAdaptiveUpdateTime(feedId: Long, lastUpdateTime: Long): Long {
        val aggregatedEntriesSinceYesterday = Entries.queryPublishedCount(feedId, lastUpdateTime - DateUtils.DAY_IN_MILLIS)
        val updateRate = if (aggregatedEntriesSinceYesterday > 0) {
            max(DateUtils.DAY_IN_MILLIS / aggregatedEntriesSinceYesterday, DateUtils.HOUR_IN_MILLIS / 2) / 2
        } else {
            val aggregatedEntriesSinceLastWeek = Entries.queryPublishedCount(feedId, lastUpdateTime - DateUtils.WEEK_IN_MILLIS)
            if (aggregatedEntriesSinceLastWeek > 0) {
                min(DateUtils.WEEK_IN_MILLIS / aggregatedEntriesSinceLastWeek, DateUtils.DAY_IN_MILLIS / 2) / 2
            } else {
                DateUtils.DAY_IN_MILLIS / 2
            }
        }
        val alignedUpdateRate = updateRate / (DateUtils.HOUR_IN_MILLIS / 4) * (DateUtils.HOUR_IN_MILLIS / 4)
        return lastUpdateTime / (DateUtils.HOUR_IN_MILLIS / 4) * (DateUtils.HOUR_IN_MILLIS / 4) + alignedUpdateRate
    }

    private fun calculateNextRepeatingUpdateTime(lastUpdateTime: Long, minutes: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastUpdateTime
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val midnight = calendar.timeInMillis
        val minutesInMillis = minutes * 60000
        return midnight + ((lastUpdateTime - midnight) / minutesInMillis + 1) * minutesInMillis
    }

    private class Feed(
            val id: Long,
            val lastUpdateTime: Long,
            val updateMode: UpdateMode
    ) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
                Feeds.ID,
                Feeds.LAST_UPDATE_TIME,
                Feeds.UPDATE_MODE
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                    id = cursor.getLong(0),
                    lastUpdateTime = cursor.getLong(1),
                    updateMode = UpdateMode.deserialize(cursor.getString(2))
            )
        }
    }

}
