package com.tughi.aggregator.services

import android.app.job.JobParameters
import android.app.job.JobService
import android.database.Cursor
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.FeedsWithoutFaviconCriteria
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class FaviconUpdateService : JobService() {

    private val jobs = mutableMapOf<Int, WeakReference<Job>>()

    override fun onStartJob(params: JobParameters): Boolean {
        GlobalScope.launch(Dispatchers.IO) {
            val feeds = Feeds.query(FeedsWithoutFaviconCriteria, Feed.QueryHelper)
            for (feed in feeds) {
                FaviconUpdateHelper.updateFavicon(feed.id)
            }
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
            job?.cancel()
        }
        return true
    }

    class Feed(val id: Long) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
                Feeds.ID
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                    id = cursor.getLong(0)
            )
        }
    }

}
