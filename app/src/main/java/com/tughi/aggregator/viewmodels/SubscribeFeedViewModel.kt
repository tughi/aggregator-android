package com.tughi.aggregator.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.UpdateMode

class SubscribeFeedViewModel() : ViewModel() {

    var updateMode = MediatorLiveData<UpdateMode>().apply { value = DefaultUpdateMode }

    class Factory() : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SubscribeFeedViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SubscribeFeedViewModel() as T
            }
            throw UnsupportedOperationException()
        }

    }

}
