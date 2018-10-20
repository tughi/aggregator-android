package com.tughi.aggregator.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.tughi.aggregator.App
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.UiEntriesGetter
import com.tughi.aggregator.data.UiEntry

class EntryListViewModel(entriesGetter: UiEntriesGetter) : ViewModel() {

    private val context = App.instance.applicationContext
    private val database = Database.from(context)

    val entries: LiveData<PagedList<UiEntry>>

    init {
        val entryDao = database.entryDao()
        val config = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setInitialLoadSizeHint(100)
                .setPageSize(50)
                .build()
        entries = LivePagedListBuilder(entriesGetter.getUiEntries(entryDao), config)
                .build()
    }

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
