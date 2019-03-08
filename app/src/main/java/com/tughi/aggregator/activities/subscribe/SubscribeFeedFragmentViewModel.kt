package com.tughi.aggregator.activities.subscribe

import android.content.ContentValues
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.DataMapper
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.FeedsRepository
import com.tughi.aggregator.data.UpdateMode

class SubscribeFeedFragmentViewModel : ViewModel() {

    val repository = FeedsRepository(
            emptyArray(),
            object : DataMapper<Feed>() {
                override fun map(data: Feed) = ContentValues().apply {
                    put(FeedsRepository.URL.name, data.url)
                    put(FeedsRepository.TITLE.name, data.title)
                    put(FeedsRepository.LINK.name, data.link)
                    put(FeedsRepository.UPDATE_MODE.name, data.updateMode.serialize())
                    put(FeedsRepository.CUSTOM_TITLE.name, data.customTitle)
                    put(FeedsRepository.LAST_UPDATE_TIME.name, 0) // TODO: fix table schema to avoid this
                    put(FeedsRepository.NEXT_UPDATE_RETRY.name, 0) // TODO: fix table schema to avoid this
                    put(FeedsRepository.NEXT_UPDATE_TIME.name, 0) // TODO: fix table schema to avoid this
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
