package com.tughi.aggregator.activities.main

import android.database.Cursor
import android.text.format.DateUtils
import androidx.collection.SparseArrayCompat
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

    private val entries = Transformations.switchMap(queryCriteria) { queryCriteria ->
        Entries.liveQuery(queryCriteria, Entry.QueryHelper())
    }

    val items: LiveData<List<Item>> = MediatorLiveData<List<Item>>().also {
        var currentJob: Job? = null

        it.addSource(entries) { entries ->
            currentJob?.run {
                cancel()
            }

            if (entries.isEmpty()) {
                currentJob = null
                it.postValue(emptyList())
            } else {
                currentJob = GlobalScope.launch {
                    if (!isActive) {
                        return@launch
                    }

                    val items = mutableListOf<Item>()

                    val entriesIterator = entries.iterator()

                    var prevEntry = entriesIterator.next()
                    items.add(Header(prevEntry))
                    items.add(prevEntry)

                    var index = 0
                    entriesIterator.forEach { entry ->
                        index += 1
                        if (entry.numericDate != prevEntry.numericDate) {
                            items.add(Header(entry))
                        } else {
                            items.add(Divider(entry, index))
                        }
                        items.add(entry)
                        prevEntry = entry
                    }

                    if (!isActive) {
                        return@launch
                    }

                    it.postValue(items)
                }
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

    interface Item {
        val id: Long
        val numericDate: Int
        val formattedDate: String
    }

    class Divider(entry: Entry, index: Int) : Item {
        override val id: Long = -4200_00_00L - index
        override val numericDate: Int = entry.numericDate
        override val formattedDate: String = entry.formattedDate
    }

    class Header(entry: Entry) : Item {
        override val id: Long = -entry.numericDate.toLong()
        override val numericDate: Int = entry.numericDate
        override val formattedDate: String = entry.formattedDate
    }

    data class Entry(
            override val id: Long,
            val feedId: Long,
            val feedTitle: String,
            val faviconUrl: String?,
            val title: String?,
            val link: String?,
            val author: String?,
            override val formattedDate: String,
            val formattedTime: String,
            val readTime: Long,
            val pinnedTime: Long,
            override val numericDate: Int
    ) : Item {
        val unread = readTime == 0L || pinnedTime != 0L

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
                Entries.TITLE
        ) {
            private val context = App.instance
            private val formattedDates = SparseArrayCompat<String>()

            override fun createRow(cursor: Cursor): Entry {
                val publishTime = cursor.getLong(5)

                val calendar = Calendar.getInstance()
                calendar.timeInMillis = publishTime
                val numericDate = calendar.get(Calendar.YEAR) * 10_000 + calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DAY_OF_MONTH)

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
