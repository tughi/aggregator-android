package com.tughi.aggregator.services

import android.app.job.JobParameters
import android.app.job.JobService
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.preferences.UpdateSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AutoUpdateService : JobService() {

    private var currentJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        if (UpdateSettings.backgroundUpdates) {
            currentJob = GlobalScope.launch {
                val feedIds = Feeds.queryOutdatedFeedIds(System.currentTimeMillis())

                val jobs = feedIds.map { feedId ->
                    async { FeedUpdater.updateFeed(feedId) }
                }
                jobs.forEach {
                    it.await()
                }
            }.also {
                it.invokeOnCompletion { error ->
                    jobFinished(params, error != null && error !is CancellationException)

                    GlobalScope.launch {
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
