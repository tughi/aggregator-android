package com.tughi.aggregator.activities.reader

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.data.EntriesQuery

class ReaderActivityViewModel(entriesQuery: EntriesQuery) : ViewModel() {

    val entries: LiveData<Array<ReaderActivityEntry>> = AppDatabase.instance.readerDao().getReaderActivityEntries(entriesQuery)

    class Factory(private val entriesQuery: EntriesQuery) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderActivityViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderActivityViewModel(entriesQuery) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
