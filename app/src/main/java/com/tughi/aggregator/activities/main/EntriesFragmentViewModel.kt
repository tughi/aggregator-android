package com.tughi.aggregator.activities.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.data.EntriesQuery
import com.tughi.aggregator.data.EntriesSortOrder
import com.tughi.aggregator.data.FeedEntriesQuery
import com.tughi.aggregator.data.MyFeedEntriesQuery
import com.tughi.aggregator.preferences.EntryListSettings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EntriesFragmentViewModel(initialEntriesQuery: EntriesQuery) : ViewModel() {

    private val sessionTime = System.currentTimeMillis()

    val entriesQuery = MutableLiveData<EntriesQuery>().apply {
        value = when (initialEntriesQuery) {
            is FeedEntriesQuery -> initialEntriesQuery.copy(sessionTime = sessionTime)
            is MyFeedEntriesQuery -> initialEntriesQuery.copy(sessionTime = sessionTime)
        }
    }

    private val databaseEntries = Transformations.switchMap(entriesQuery) { entriesQuery ->
        AppDatabase.instance.mainDao().getEntriesFragmentEntries(entriesQuery)
    }

    private val transformedEntries = MediatorLiveData<List<EntriesFragmentEntry>>().also {
        var currentJob: Job? = null

        it.addSource(databaseEntries) { databaseEntries ->
            currentJob?.run {
                cancel()
            }

            currentJob = GlobalScope.launch {
                if (!isActive) {
                    return@launch
                }

                val oldEntries = it.value
                if (oldEntries != null && oldEntries.size == databaseEntries.size * 2) {
                    var changed = false
                    for (index in 0 until databaseEntries.size) {
                        if (oldEntries[index * 2 + 1] != databaseEntries[index]) {
                            changed = true
                            break
                        }
                    }
                    if (!changed) {
                        return@launch
                    }
                }

                if (!isActive) {
                    return@launch
                }

                val newEntries = Array(databaseEntries.size * 2) { index ->
                    when {
                        index == 0 -> databaseEntries[0].let { entry ->
                            entry.copy(
                                    id = -entry.id,
                                    readTime = 0,
                                    type = EntriesFragmentEntryType.HEADER
                            )
                        }
                        index % 2 == 0 -> databaseEntries[index / 2].let { entry ->
                            entry.copy(
                                    id = -entry.id,
                                    readTime = 0,
                                    type = if (entry.formattedDate != databaseEntries[index / 2 - 1].formattedDate) EntriesFragmentEntryType.HEADER else EntriesFragmentEntryType.DIVIDER
                            )
                        }
                        else -> databaseEntries[index / 2]
                    }
                }

                if (!isActive) {
                    return@launch
                }

                it.postValue(newEntries.asList())
            }
        }
    }

    val entries: LiveData<List<EntriesFragmentEntry>>
        get() = transformedEntries

    fun changeEntriesSortOrder(entriesSortOrder: EntriesSortOrder) {
        EntryListSettings.entriesSortOrder = entriesSortOrder

        transformedEntries.value = null
        entriesQuery.value?.let { value ->
            entriesQuery.value = when (value) {
                is FeedEntriesQuery -> value.copy(sortOrder = entriesSortOrder)
                is MyFeedEntriesQuery -> value.copy(sortOrder = entriesSortOrder)
            }
        }
    }

    fun changeShowRead(showRead: Boolean) {
        transformedEntries.value = null
        entriesQuery.value?.let { value ->
            entriesQuery.value = when (value) {
                is FeedEntriesQuery -> value.copy(sessionTime = if (showRead) 0 else sessionTime)
                is MyFeedEntriesQuery -> value.copy(sessionTime = if (showRead) 0 else sessionTime)
            }
        }
    }

    class Factory(private val initialEntriesQuery: EntriesQuery) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EntriesFragmentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EntriesFragmentViewModel(initialEntriesQuery) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
