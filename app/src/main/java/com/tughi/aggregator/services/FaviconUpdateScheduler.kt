package com.tughi.aggregator.services

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import com.tughi.aggregator.App
import com.tughi.aggregator.FEED_JOB__INCREMENT__FAVICON_UPDATER
import com.tughi.aggregator.FEED_JOB__MULTIPLIER

object FaviconUpdateScheduler {

    const val FEED_ID = "feed_id"

    fun schedule(feedId: Long) {
        val context: Context = App.instance
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        val jobId = (feedId * FEED_JOB__MULTIPLIER + FEED_JOB__INCREMENT__FAVICON_UPDATER).toInt()
        val jobInfo = JobInfo.Builder(jobId, ComponentName(context, FaviconUpdateService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(0)
                .setPersisted(true)
                .setExtras(PersistableBundle().apply {
                    putLong(FEED_ID, feedId)
                })
                .build()

        jobScheduler.schedule(jobInfo)
    }

}
