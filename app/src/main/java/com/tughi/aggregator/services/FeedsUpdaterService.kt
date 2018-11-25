package com.tughi.aggregator.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.text.format.DateUtils
import com.tughi.aggregator.App
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.UpdateSettings
import com.tughi.aggregator.utilities.JOB_SERVICE_FEEDS_UPDATER_SCHEDULER
import kotlinx.coroutines.*

class FeedsUpdaterService : JobService() {

    companion object {
        fun schedule() {
            if (!UpdateSettings.backgroundUpdates) {
                return
            }

            val context = App.instance
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val jobInfo = JobInfo.Builder(JOB_SERVICE_FEEDS_UPDATER_SCHEDULER, ComponentName(context, FeedsUpdaterSchedulerService::class.java))
                    .setMinimumLatency(DateUtils.MINUTE_IN_MILLIS)
                    .setPersisted(true)
                    .build()

            jobScheduler.schedule(jobInfo)
        }
    }

    private var currentJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        if (UpdateSettings.backgroundUpdates) {
            currentJob = GlobalScope.launch(Dispatchers.IO) {
                val feeds = AppDatabase.instance.feedDao().queryUpdatableFeeds(System.currentTimeMillis())

                val jobs = feeds.map { feedId ->
                    async { FeedUpdater.updateFeed(feedId) }
                }
                jobs.forEach {
                    it.await()
                }
            }.also {
                it.invokeOnCompletion { error ->
                    jobFinished(params, error != null && error !is CancellationException)
                }
            }

            return true
        }

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        currentJob?.cancel()

        return true
    }

}
