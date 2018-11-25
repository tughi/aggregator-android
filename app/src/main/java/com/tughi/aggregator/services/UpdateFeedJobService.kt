package com.tughi.aggregator.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.tughi.aggregator.App
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.utilities.JOB_SERVICE_UPDATE_FEEDS
import kotlinx.coroutines.*
import kotlin.math.max

class UpdateFeedJobService : JobService() {

    companion object {
        fun schedule() {
            val nextUpdateTime = AppDatabase.instance.feedDao().queryNextUpdateTime() ?: return
            val nextUpdateDelay = nextUpdateTime - System.currentTimeMillis()

            val context: Context = App.instance
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val jobInfo = JobInfo.Builder(JOB_SERVICE_UPDATE_FEEDS, ComponentName(context, UpdateFeedJobService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(max(0, nextUpdateDelay))
                    .setPersisted(true)
                    .build()

            Log.d(UpdateFeedJob::class.java.name, "Schedule update with delay: $nextUpdateDelay")

            // TODO: find a solution where this doesn't cancel the current job
            jobScheduler.schedule(jobInfo)
        }
    }

    private var currentJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        currentJob = GlobalScope.launch(Dispatchers.IO) {
            val feeds = AppDatabase.instance.feedDao().queryUpdatableFeeds(System.currentTimeMillis())

            val jobs = feeds.map { feedId ->
                async { UpdateFeedJob.updateFeed(feedId) }
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

    override fun onStopJob(params: JobParameters?): Boolean {
        currentJob?.cancel()

        return true
    }

}
