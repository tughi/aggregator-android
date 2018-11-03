package com.tughi.aggregator.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.App
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.UiEntriesGetter
import com.tughi.aggregator.data.UiEntry
import com.tughi.aggregator.data.UiEntryType

class EntryListViewModel(entriesGetter: UiEntriesGetter) : ViewModel() {

    private val databaseEntries: LiveData<List<UiEntry>> = entriesGetter.getUiEntries(Database.from(App.instance).entryDao())

    val entries: LiveData<List<UiEntry>> = MediatorLiveData<List<UiEntry>>().apply {
        addSource(databaseEntries) { list ->
            // TODO: move to separate thread
            if (list.isEmpty()) {
                value = list
            } else {
                val newList = ArrayList<UiEntry>(list.size * 2)

                val firstEntry = list.first()
                newList.add(firstEntry.copy(type = UiEntryType.HEADER))
                newList.add(firstEntry)

                var previousEntry = firstEntry
                for (index in 1 until list.size) {
                    val entry = list[index]

                    newList.add(entry.copy(
                            readTime = 0,
                            type = if (entry.formattedDate == previousEntry.formattedDate) UiEntryType.DIVIDER else UiEntryType.HEADER
                    ))
                    newList.add(entry)

                    previousEntry = entry
                }

                value = newList
            }
        }
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
