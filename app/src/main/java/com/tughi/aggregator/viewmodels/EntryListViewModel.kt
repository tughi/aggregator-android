package com.tughi.aggregator.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.UiEntry

class EntryListViewModel(application: Application) : AndroidViewModel(application) {

    val entries: LiveData<PagedList<UiEntry>>

    init {
        val entryDao = Database.from(application).entryDao()
        val config = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setInitialLoadSizeHint(100)
                .setPageSize(50)
                .build()
        entries = LivePagedListBuilder(entryDao.getUiEntries(), config)
                .build()
    }

}
