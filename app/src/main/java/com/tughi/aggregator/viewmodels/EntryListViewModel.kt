package com.tughi.aggregator.viewmodels

import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.*
import com.tughi.aggregator.App
import com.tughi.aggregator.data.AppDatabase
import com.tughi.aggregator.data.UiEntriesGetter
import com.tughi.aggregator.data.UiEntry
import com.tughi.aggregator.data.UiEntryType

class EntryListViewModel(entriesGetter: UiEntriesGetter) : ViewModel() {

    companion object {
        private const val MSG_PROCESS_DATABASE_ENTRIES = 1
    }

    private val databaseEntries: LiveData<Array<UiEntry>> = entriesGetter.getUiEntries(AppDatabase.from(App.instance).entryDao())
    private val processedDatabaseEntries = MutableLiveData<Array<UiEntry>>()

    private val handlerThread = HandlerThread(javaClass.simpleName).also { it.start() }
    private val handler = Handler(handlerThread.looper, Handler.Callback {
        when (it.what) {
            MSG_PROCESS_DATABASE_ENTRIES -> {
                @Suppress("UNCHECKED_CAST")
                processDatabaseEntries(it.obj as Array<UiEntry>)
            }
        }
        return@Callback true
    })

    val entries: LiveData<List<UiEntry>> = MediatorLiveData<List<UiEntry>>().apply {
        addSource(databaseEntries) { databaseEntries ->
            if (databaseEntries.isEmpty()) {
                value = databaseEntries.asList()
            } else {
                handler.removeMessages(MSG_PROCESS_DATABASE_ENTRIES)
                handler.sendMessage(handler.obtainMessage(MSG_PROCESS_DATABASE_ENTRIES, databaseEntries))
            }
        }
        addSource(processedDatabaseEntries) { entries ->
            value = entries.asList()
        }
    }

    private fun processDatabaseEntries(databaseEntries: Array<UiEntry>) {
        val oldEntries = processedDatabaseEntries.value
        if (oldEntries != null && oldEntries.size == databaseEntries.size * 2) {
            var changed = false
            for (index in 0 until databaseEntries.size) {
                if (oldEntries[index * 2 + 1] != databaseEntries[index]) {
                    changed = true
                    break
                }
            }
            if (!changed) {
                return
            }
        }


        val newEntries = Array(databaseEntries.size * 2) { index ->
            when {
                index == 0 -> databaseEntries[0].copy(readTime = 0, type = UiEntryType.HEADER)
                index % 2 == 0 -> databaseEntries[index / 2].let {
                    it.copy(readTime = 0, type = if (it.formattedDate != databaseEntries[index / 2 - 1].formattedDate) UiEntryType.HEADER else UiEntryType.DIVIDER)
                }
                else -> databaseEntries[index / 2]
            }
        }

        processedDatabaseEntries.postValue(newEntries)
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
