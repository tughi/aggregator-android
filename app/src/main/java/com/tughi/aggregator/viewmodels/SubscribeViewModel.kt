package com.tughi.aggregator.viewmodels

import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tughi.aggregator.data.Feed
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.AnkoLogger

class SubscribeViewModel : ViewModel(), AnkoLogger {

    val busy = MutableLiveData<Boolean>()
    val message = MutableLiveData<String>()
    val feeds = MutableLiveData<List<Feed>>()

    private var currentFindTask: FindTask? = null

    fun findFeeds(url: String) {
        currentFindTask?.cancel()

        busy.value = true

        FindTask(this).also { currentFindTask = it }.execute(url)
    }

    override fun onCleared() {
        currentFindTask?.cancel()
    }

    class FindTask(private val viewModel: SubscribeViewModel) : AsyncTask<Any, Void, Boolean>() {

        private var requestCall: Call? = null
        private var content: String? = null

        fun cancel() {
            cancel(false)
            requestCall?.cancel()
        }

        override fun doInBackground(vararg params: Any?): Boolean {
            val url = params[0] as String

            val request = Request.Builder()
                    .url(url)
                    .build()

            if (!isCancelled) {
                val response = OkHttpClient().newCall(request).also { requestCall = it }.execute()

                if (!isCancelled) {
                    val body = response.body()

                    if (body != null) {
                        content = body.string()

                        Log.d(viewModel.loggerTag, "Content: " + content)
                        Log.d(viewModel.loggerTag, "ContentLength: " + body.contentLength())
                        Log.d(viewModel.loggerTag, "ContentType: " + body.contentType())

                        return true
                    }
                }
            }

            return false
        }

        override fun onPostExecute(success: Boolean?) {
            val content = content

            if (success == true && content != null) {
                viewModel.message.value = content
            }

            viewModel.busy.value = false
        }

    }

}