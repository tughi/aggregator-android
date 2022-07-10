package com.tughi.aggregator.activities.subscribe

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.DefaultCleanupMode
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.UpdateMode

class SubscribeFeedFragmentViewModel : ViewModel() {

    var cleanupMode = MutableLiveData<CleanupMode>().apply { value = DefaultCleanupMode }
    var updateMode = MutableLiveData<UpdateMode>().apply { value = DefaultUpdateMode }

    class Feed(val url: String, val title: String, val link: String?, val updateMode: UpdateMode, val customTitle: String?)

    class Factory : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SubscribeFeedFragmentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SubscribeFeedFragmentViewModel() as T
            }
            throw UnsupportedOperationException()
        }

    }

}
