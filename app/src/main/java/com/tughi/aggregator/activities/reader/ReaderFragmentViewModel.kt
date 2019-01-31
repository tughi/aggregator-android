package com.tughi.aggregator.activities.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.AppDatabase

internal class ReaderFragmentViewModel(entryId: Long, entryReadTime: Long) : ViewModel() {

    val entry = AppDatabase.instance.readerDao().getReaderFragmentEntry(entryId)

    class Factory(private val entryId: Long, private val entryReadTime: Long) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderFragmentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderFragmentViewModel(entryId, entryReadTime) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
