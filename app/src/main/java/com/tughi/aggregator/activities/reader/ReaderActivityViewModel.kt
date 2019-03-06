package com.tughi.aggregator.activities.reader

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.data.EntriesRepository

class ReaderActivityViewModel(queryCriteria: EntriesRepository.QueryCriteria) : ViewModel() {

    val entries: LiveData<Array<ReaderActivityEntry>> = AppDatabase.instance.readerDao().getReaderActivityEntries(queryCriteria)

    class Factory(private val queryCriteria: EntriesRepository.QueryCriteria) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderActivityViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderActivityViewModel(queryCriteria) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
