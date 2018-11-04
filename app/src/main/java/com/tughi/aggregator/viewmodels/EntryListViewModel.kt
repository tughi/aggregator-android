package com.tughi.aggregator.viewmodels

import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.*
import com.tughi.aggregator.App
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.UiEntriesGetter
import com.tughi.aggregator.data.UiEntry
import com.tughi.aggregator.data.UiEntryType

class EntryListViewModel(entriesGetter: UiEntriesGetter) : ViewModel() {

    companion object {
        private const val MSG_PROCESS_DATABASE_ENTRIES = 1
    }

    private val databaseEntries: LiveData<List<UiEntry>> = entriesGetter.getUiEntries(Database.from(App.instance).entryDao())
    private val processedDatabaseEntries = MutableLiveData<List<UiEntry>>()

    private val handlerThread = HandlerThread(javaClass.simpleName).also { it.start() }
    private val handler = Handler(handlerThread.looper, Handler.Callback {
        when (it.what) {
            MSG_PROCESS_DATABASE_ENTRIES -> {
                @Suppress("UNCHECKED_CAST")
                processDatabaseEntries(it.obj as List<UiEntry>)
            }
        }
        return@Callback true
    })

    val entries: LiveData<List<UiEntry>> = MediatorLiveData<List<UiEntry>>().apply {
        addSource(databaseEntries) { list ->
            if (list.isEmpty()) {
                value = list
            } else {
                handler.removeMessages(MSG_PROCESS_DATABASE_ENTRIES)
                handler.sendMessage(handler.obtainMessage(MSG_PROCESS_DATABASE_ENTRIES, list))
            }
        }
        addSource(processedDatabaseEntries, ::setValue)
    }

    private fun processDatabaseEntries(list: List<UiEntry>) {
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

        val oldList = processedDatabaseEntries.value
        if (oldList == null || oldList.size != newList.size) {
            processedDatabaseEntries.postValue(newList)
        } else {
            var changed = false
            for (index in 0 until newList.size) {
                if (oldList[index] != newList[index]) {
                    changed = true
                    break
                }
            }
            if (changed) {
                processedDatabaseEntries.postValue(newList)
            }
        }
    }

    override fun onCleared() {
        handlerThread.quit()

        super.onCleared()
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
