package com.tughi.aggregator.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.App
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.UiEntriesGetter
import com.tughi.aggregator.data.UiEntry

class EntryListViewModel(entriesGetter: UiEntriesGetter) : ViewModel() {

    val entries: LiveData<List<UiEntry>> = entriesGetter.getUiEntries(Database.from(App.instance).entryDao())

    class Factory(private val entriesGetter: UiEntriesGetter) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EntryListViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EntryListViewModel(entriesGetter) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
