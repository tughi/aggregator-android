package com.tughi.aggregator.activities.main

import android.database.Cursor
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.App
import com.tughi.aggregator.data.DataMapper
import com.tughi.aggregator.data.EntriesQuery
import com.tughi.aggregator.data.EntriesRepository
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

    private val repository = EntriesRepository(
            arrayOf(
                    EntriesRepository.Column.ID,
                    EntriesRepository.Column.FEED_ID,
                    EntriesRepository.Column.AUTHOR,
                    EntriesRepository.Column.FEED_FAVICON_URL,
                    EntriesRepository.Column.FEED_TITLE,
                    EntriesRepository.Column.PUBLISH_TIME,
                    EntriesRepository.Column.LINK,
                    EntriesRepository.Column.PINNED_TIME,
                    EntriesRepository.Column.READ_TIME,
                    EntriesRepository.Column.TITLE,
                    EntriesRepository.Column.TYPE
            ),
            object : DataMapper<EntriesFragmentEntry> {
                private val context = App.instance

                override fun map(cursor: Cursor) = EntriesFragmentEntry(
                        id = cursor.getLong(0),
                        feedId = cursor.getLong(1),
                        author = cursor.getString(2),
                        faviconUrl = cursor.getString(3),
                        feedTitle = cursor.getString(4),
                        formattedDate = DateUtils.formatDateTime(context, cursor.getLong(5), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR),
                        formattedTime = DateUtils.formatDateTime(context, cursor.getLong(5), DateUtils.FORMAT_SHOW_TIME),
                        link = cursor.getString(6),
                        pinnedTime = cursor.getLong(7),
                        readTime = cursor.getLong(8),
                        title = cursor.getString(9),
                        type = EntriesFragmentEntryType.valueOf(cursor.getString(10))
                )
            }
    )

    private val storedEntries = Transformations.switchMap(entriesQuery) { entriesQuery ->
        repository.liveQuery(when (entriesQuery) {
            is FeedEntriesQuery -> EntriesRepository.FeedEntriesCriteria(entriesQuery.feedId, entriesQuery.sessionTime, entriesQuery.sortOrder)
            is MyFeedEntriesQuery -> EntriesRepository.MyFeedCriteria(entriesQuery.sessionTime, entriesQuery.sortOrder)
        })
    }

    private val transformedEntries = MediatorLiveData<List<EntriesFragmentEntry>>().also {
        var currentJob: Job? = null

        it.addSource(storedEntries) { storedEntries ->
            currentJob?.run {
                cancel()
            }

            currentJob = GlobalScope.launch {
                if (!isActive) {
                    return@launch
                }

                val oldEntries = it.value
                if (oldEntries != null && oldEntries.size == storedEntries.size * 2) {
                    var changed = false
                    for (index in 0 until storedEntries.size) {
                        if (oldEntries[index * 2 + 1] != storedEntries[index]) {
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

                val newEntries = Array(storedEntries.size * 2) { index ->
                    when {
                        index == 0 -> storedEntries[0].let { entry ->
                            entry.copy(
                                    id = -entry.id,
                                    readTime = 0,
                                    type = EntriesFragmentEntryType.HEADER
                            )
                        }
                        index % 2 == 0 -> storedEntries[index / 2].let { entry ->
                            entry.copy(
                                    id = -entry.id,
                                    readTime = 0,
                                    type = if (entry.formattedDate != storedEntries[index / 2 - 1].formattedDate) EntriesFragmentEntryType.HEADER else EntriesFragmentEntryType.DIVIDER
                            )
                        }
                        else -> storedEntries[index / 2]
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
