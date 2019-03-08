package com.tughi.aggregator.activities.subscribe

import android.content.ContentValues
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.Repository
import com.tughi.aggregator.data.UpdateMode

class SubscribeFeedFragmentViewModel : ViewModel() {

    val repository = Feeds(
            emptyArray(),
            object : Repository.DataMapper<Feed>() {
                override fun map(data: Feed) = ContentValues().apply {
                    put(Feeds.URL, data.url)
                    put(Feeds.TITLE, data.title)
                    put(Feeds.LINK, data.link)
                    put(Feeds.UPDATE_MODE, data.updateMode.serialize())
                    put(Feeds.CUSTOM_TITLE, data.customTitle)
                    put(Feeds.LAST_UPDATE_TIME, 0) // TODO: fix table schema to avoid this
                    put(Feeds.NEXT_UPDATE_RETRY, 0) // TODO: fix table schema to avoid this
                    put(Feeds.NEXT_UPDATE_TIME, 0) // TODO: fix table schema to avoid this
                }
            }
    )

    var updateMode = MutableLiveData<UpdateMode>().apply { value = DefaultUpdateMode }

    class Feed(val url: String, val title: String, val link: String?, val updateMode: UpdateMode, val customTitle: String?)

    class Factory : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SubscribeFeedFragmentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SubscribeFeedFragmentViewModel() as T
            }
            throw UnsupportedOperationException()
        }

    }

}
