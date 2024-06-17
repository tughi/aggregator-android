package com.tughi.aggregator.activities.subscribe

import android.database.Cursor
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tughi.aggregator.App
import com.tughi.aggregator.R
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.feeds.FeedsFinder
import com.tughi.aggregator.utilities.Failure
import com.tughi.aggregator.utilities.Http
import com.tughi.aggregator.utilities.Success
import com.tughi.aggregator.utilities.content
import com.tughi.aggregator.utilities.onFailure
import com.tughi.aggregator.utilities.then
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException

class SubscribeSearchFragmentViewModel : ViewModel() {

    val state = MutableLiveData<State>().apply {
        value = State(null, false, emptyList(), null)
    }

    private var currentFindJob: Job? = null

    val hasNewsFeed = MediatorLiveData(true).apply {
        addSource(
            Feeds.liveQueryOne(
                Feeds.UrlCriteria(App.instance.getString(R.string.app_feed)),
                object : Feeds.QueryHelper<Long>(Feeds.ID) {
                    override fun createRow(cursor: Cursor): Long = cursor.getLong(0)
                },
            )
        ) {
            value = it != null
        }
    }

    fun findFeeds(url: String) {
        currentFindJob?.cancel()

        state.value = State(url, true, emptyList(), null)

        currentFindJob = contentScope.launch {
            onFindFeeds(fixUrl(url))
        }
    }

    override fun onCleared() {
        currentFindJob?.cancel()
    }

    private fun fixUrl(url: String): String {
        if (!url.contains(Regex("^\\w+://"))) {
            return "http://$url"
        }
        return url
    }

    private suspend fun onFindFeeds(url: String) {
        val feeds = arrayListOf<Feed>()
        val stateValue = state.value?.cloneWith(loading = true, feeds = feeds) ?: return
        Http.request(url)
            .then { response ->
                if (!response.isSuccessful) {
                    return@then Failure(IllegalStateException("Server response: ${response.code} ${response.message}"))
                }

                val responseBody = response.body
                if (responseBody != null) {
                    val content = responseBody.content()

                    val listener = object : FeedsFinder.Listener {
                        override fun onFeedFound(url: String, title: String, link: String?) {
                            feeds.add(
                                Feed(
                                    url = url,
                                    title = title,
                                    link = link
                                )
                            )
                            state.postValue(stateValue)
                        }
                    }

                    FeedsFinder(listener).find(content, response.request.url.toString())

                    if (feeds.isEmpty()) {
                        return@then Failure(IllegalStateException("No feeds found"))
                    } else {
                        state.postValue(stateValue.cloneWith(loading = false))
                        return@then Success(feeds)
                    }
                } else {
                    return@then Failure(IllegalStateException("No content"))
                }
            }
            .onFailure { cause ->
                val message = when {
                    cause is MalformedURLException -> "Invalid URL"
                    cause is NoRouteToHostException -> "Could not open a connection"
                    cause is SocketTimeoutException -> "Timeout error... Please try again"
                    cause.message != null -> cause.message
                    else -> "Unexpected error: ${cause::class.java.simpleName}"
                }
                state.postValue(stateValue.cloneWith(loading = false, message = message))
            }
    }

    class Feed(
        val url: String,
        val title: String,
        val link: String?
    )

    data class State(val url: String?, val loading: Boolean, val feeds: List<Feed>, val message: String?) {
        fun cloneWith(loading: Boolean? = null, feeds: List<Feed>? = null, message: String? = null): State {
            return State(
                url = this.url,
                loading = loading ?: this.loading,
                feeds = feeds ?: this.feeds,
                message = message ?: this.message
            )
        }
    }

}
