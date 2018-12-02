package com.tughi.aggregator.services

import android.app.job.JobParameters
import android.app.job.JobService
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.UpdateSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AutoUpdateService : JobService() {

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

                    GlobalScope.launch(Dispatchers.IO) {
                        AutoUpdateScheduler.schedule()
                    }
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
