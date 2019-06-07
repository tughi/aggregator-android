package com.tughi.aggregator.services

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class FaviconUpdateService : JobService() {

    private val jobs = mutableMapOf<Int, WeakReference<Job>>()

    override fun onStartJob(params: JobParameters): Boolean {
        val feedId = params.extras.getLong(FaviconUpdateScheduler.FEED_ID)

        GlobalScope.launch(Dispatchers.IO) {
            FaviconUpdateHelper.updateFavicon(feedId)
        }.also { job ->
            jobs[params.jobId] = WeakReference(job)

            job.invokeOnCompletion { error ->
                jobFinished(params, error != null && error !is CancellationException)
            }
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val jobReference = jobs[params.jobId]
        if (jobReference != null) {
            val job = jobReference.get()
            if (job != null) {
                job.cancel()
            }
        }

        return true
    }

}