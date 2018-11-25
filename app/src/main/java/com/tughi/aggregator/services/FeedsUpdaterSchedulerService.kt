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
import com.tughi.aggregator.utilities.JOB_SERVICE_FEEDS_UPDATER
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.max

class FeedsUpdaterSchedulerService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        GlobalScope.launch {
            val nextUpdateTime = AppDatabase.instance.feedDao().queryNextUpdateTime() ?: return@launch
            val nextUpdateDelay = nextUpdateTime - System.currentTimeMillis()

            val context: Context = App.instance
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val jobInfo = JobInfo.Builder(JOB_SERVICE_FEEDS_UPDATER, ComponentName(context, FeedsUpdaterService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(max(0, nextUpdateDelay))
                    .setPersisted(true)
                    .build()

            Log.d(FeedsUpdaterSchedulerService::class.java.name, "Schedule next update with a ${nextUpdateDelay}ms delay")

            jobScheduler.schedule(jobInfo)

        }.invokeOnCompletion { error ->
            jobFinished(params, error != null)
        }

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }

}
