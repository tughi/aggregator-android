package com.tughi.aggregator.services

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import com.tughi.aggregator.App
import com.tughi.aggregator.JOB_SERVICE_FAVICONS_UPDATER

object FaviconUpdateScheduler {

    fun schedule() {
        val context: Context = App.instance
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        val jobInfo = JobInfo.Builder(JOB_SERVICE_FAVICONS_UPDATER, ComponentName(context, FaviconUpdateService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(0)
                .setPersisted(true)
                .build()

        jobScheduler.schedule(jobInfo)
    }

}
