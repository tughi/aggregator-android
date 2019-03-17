package com.tughi.aggregator.activities.subscribe

import android.os.AsyncTask
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tughi.aggregator.feeds.FeedsFinder
import com.tughi.aggregator.utilities.Http
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException

class SubscribeSearchFragmentViewModel : ViewModel() {

    val state = MutableLiveData<State>().apply {
        value = State(null, false, emptyList(), null)
    }

    private var currentFindTask: FindTask? = null

    fun findFeeds(url: String) {
        currentFindTask?.cancel()

        state.value = State(url, true, emptyList(), null)

        FindTask(this).also { currentFindTask = it }.execute(url)
    }

    override fun onCleared() {
        currentFindTask?.cancel()
    }

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

    class FindTask(private val viewModel: SubscribeSearchFragmentViewModel) : AsyncTask<Any, State, State>(), FeedsFinder.Listener {

        private val feeds = arrayListOf<Feed>()
        private var state = viewModel.state.value!!.cloneWith(feeds = feeds)
        private var requestCall: Call? = null

        fun cancel() {
            cancel(false)
            requestCall?.cancel()
        }

        override fun doInBackground(vararg params: Any?): State {
            var url = params[0] as String

            if (!url.contains(Regex("^\\w+://"))) {
                url = "http://$url"
            }

            val request = Request.Builder().apply {
                try {
                    url(url)
                } catch (exception: IllegalArgumentException) {
                    return state.cloneWith(loading = false, message = "Invalid URL")
                }
            }.build()

            if (!isCancelled) {
                val response: Response?
                try {
                    response = Http.client.newCall(request).also { requestCall = it }.execute()
                } catch (exception: IOException) {
                    return when (exception) {
                        is NoRouteToHostException -> {
                            state.cloneWith(loading = false, message = "Could not open a connection")
                        }
                        is SocketTimeoutException -> {
                            state.cloneWith(loading = false, message = "Timeout error... Please try again")
                        }
                        else -> {
                            state.cloneWith(loading = false, message = "Unexpected error: ${exception::class.java.simpleName}")
                        }
                    }
                }

                if (!response.isSuccessful) {
                    return state.cloneWith(loading = false, message = "Server response: ${response.code()} ${response.message()}")
                }

                if (!isCancelled) {
                    val body = response.body()
                    val content = body?.charStream()
                    if (content != null) {
                        try {
                            FeedsFinder(this).find(content, response.request().url().toString())
                        } catch (exception: Exception) {
                            publishProgress(state.cloneWith(message = exception.localizedMessage))
                        }
                    }
                }
            }

            val state = state
            return state.cloneWith(loading = false, message = if (state.feeds.isEmpty()) "No feeds found" else null)
        }

        override fun onProgressUpdate(vararg values: State?) {
            viewModel.state.value = values[0].also { state = it!! }
        }

        override fun onPostExecute(result: State?) {
            onProgressUpdate(result)
        }

        override fun onFeedFound(url: String, title: String, link: String?) {
            feeds.add(Feed(
                    url = url,
                    title = title,
                    link = link
            ))
            publishProgress(state.cloneWith())
        }
    }

    class Feed(
            val url: String,
            val title: String,
            val link: String?
    )

}
