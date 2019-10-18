package com.tughi.aggregator.activities.main

import android.database.Cursor
import android.text.format.DateUtils
import androidx.collection.LongSparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.App
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.preferences.EntryListSettings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class EntriesFragmentViewModel(initialQueryCriteria: Entries.EntriesQueryCriteria) : ViewModel() {

    private val sessionTime = initialQueryCriteria.sessionTime

    val queryCriteria = MutableLiveData<Entries.EntriesQueryCriteria>().apply {
        value = initialQueryCriteria.copy(sessionTime = if (EntryListSettings.showReadEntries) 0 else sessionTime)
    }

    private val storedEntries = Transformations.switchMap(queryCriteria) { queryCriteria ->
        Entries.liveQuery(queryCriteria, Entry.QueryHelper())
    }

    val entries: LiveData<List<Entry>> = MediatorLiveData<List<Entry>>().also {
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
                    for (index in storedEntries.indices) {
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

                var counter = 0
                val newEntries = Array(storedEntries.size * 2) { index ->
                    when {
                        index == 0 -> storedEntries[0].let { entry ->
                            entry.copy(
                                    id = -entry.numericDate,
                                    readTime = 0,
                                    type = EntriesFragmentEntryType.HEADER
                            )
                        }
                        index % 2 == 0 -> storedEntries[index / 2].let { entry ->
                            if (entry.numericDate != storedEntries[index / 2 - 1].numericDate) {
                                counter = 0
                                entry.copy(
                                        id = -entry.numericDate,
                                        readTime = 0,
                                        type = EntriesFragmentEntryType.HEADER
                                )
                            } else {
                                counter++
                                entry.copy(
                                        id = -entry.numericDate - counter,
                                        readTime = 0,
                                        type = EntriesFragmentEntryType.DIVIDER
                                )
                            }
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

    fun changeSortOrder(sortOrder: Entries.SortOrder) {
        EntryListSettings.entriesSortOrder = sortOrder

        queryCriteria.value?.let { value ->
            queryCriteria.value = value.copy(sortOrder = sortOrder)
        }
    }

    fun changeShowRead(showRead: Boolean) {
        EntryListSettings.showReadEntries = showRead

        queryCriteria.value?.let { value ->
            queryCriteria.value = value.copy(sessionTime = if (showRead) 0 else sessionTime)
        }
    }

    data class Entry(
            val id: Long,
            val feedId: Long,
            val feedTitle: String,
            val faviconUrl: String?,
            val title: String?,
            val link: String?,
            val author: String?,
            val formattedDate: String,
            val formattedTime: String,
            val readTime: Long,
            val pinnedTime: Long,
            val type: EntriesFragmentEntryType,
            val numericDate: Long
    ) {
        class QueryHelper : Entries.QueryHelper<Entry>(
                Entries.ID,
                Entries.FEED_ID,
                Entries.AUTHOR,
                Entries.FEED_FAVICON_URL,
                Entries.FEED_TITLE,
                Entries.PUBLISH_TIME,
                Entries.LINK,
                Entries.PINNED_TIME,
                Entries.READ_TIME,
                Entries.TITLE,
                Entries.TYPE
        ) {
            private val context = App.instance
            private val formattedDates = LongSparseArray<String>()

            override fun createRow(cursor: Cursor): Entry {
                val publishTime = cursor.getLong(5)

                val calendar = Calendar.getInstance()
                calendar.timeInMillis = publishTime
                val numericDate = (calendar.get(Calendar.YEAR) * 10_000 + calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DAY_OF_MONTH)) * 100_000L

                var formattedDate = formattedDates.get(numericDate)
                if (formattedDate == null) {
                    formattedDate = DateUtils.formatDateTime(context, publishTime, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR)
                    formattedDates.put(numericDate, formattedDate)
                }

                return Entry(
                        id = cursor.getLong(0),
                        feedId = cursor.getLong(1),
                        author = cursor.getString(2),
                        faviconUrl = cursor.getString(3),
                        feedTitle = cursor.getString(4),
                        formattedDate = formattedDate!!,
                        formattedTime = DateUtils.formatDateTime(context, publishTime, DateUtils.FORMAT_SHOW_TIME),
                        link = cursor.getString(6),
                        pinnedTime = cursor.getLong(7),
                        readTime = cursor.getLong(8),
                        title = cursor.getString(9),
                        type = EntriesFragmentEntryType.valueOf(cursor.getString(10)),
                        numericDate = numericDate
                )
            }
        }
    }

    class Factory(private val initialQueryCriteria: Entries.EntriesQueryCriteria) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EntriesFragmentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EntriesFragmentViewModel(initialQueryCriteria) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
