package com.tughi.aggregator.activities.main

import android.database.Cursor
import android.text.format.DateUtils
import android.util.Log
import androidx.collection.SparseArrayCompat
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.App
import com.tughi.aggregator.BuildConfig
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.preferences.EntryListSettings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max

class EntriesFragmentViewModel(initialQueryCriteria: Entries.EntriesQueryCriteria) : ViewModel() {

    private val sessionTime = initialQueryCriteria.sessionTime

    val queryCriteria = MutableLiveData<Entries.EntriesQueryCriteria>().apply {
        value = initialQueryCriteria.copy(sessionTime = if (EntryListSettings.showReadEntries) 0 else sessionTime)
    }

    val itemsRangeSize = 15 * 6 // must be a factor of 6

    val itemsRangeStart = MutableLiveData<Int>().apply {
        value = 0
    }

    val items = MediatorLiveData<LoadedItems>().also {
        val liveEntriesCount = Transformations.switchMap(queryCriteria) { queryCriteria ->
            Entries.liveQueryCount(queryCriteria, Entry.QueryHelper)
        }

        it.addSource(liveEntriesCount) { entriesCount ->
            val itemsRangeStart = itemsRangeStart.value
            if (itemsRangeStart != null) {
                val itemsCount = entriesCount * 2
                val pageSize = itemsRangeSize / 3
                val pages = ceil(itemsCount / pageSize.toDouble()).toInt()
                val maxItemsRangeStart = max((pages - 3) * pageSize, 0)
                if (itemsRangeStart > maxItemsRangeStart) {
                    this.itemsRangeStart.value = maxItemsRangeStart
                } else {
                    loadItemsRange(itemsCount, itemsRangeStart)
                }
            }
        }

        it.addSource(itemsRangeStart) { itemsRangeStart ->
            val entriesCount = liveEntriesCount.value
            if (entriesCount != null) {
                loadItemsRange(entriesCount * 2, itemsRangeStart)
            }
        }
    }

    private var currentItemsLoaderJob: Job? = null

    private fun loadItemsRange(itemsCount: Int, itemsRangeStart: Int) {
        currentItemsLoaderJob?.apply { cancel() }

        val queryLimit = itemsRangeSize / 2
        val queryOffset = itemsRangeStart / 2
        val queryCriteria = queryCriteria.value!!.copy(limit = queryLimit, offset = queryOffset)

        if (BuildConfig.DEBUG) {
            Log.d(javaClass.name, "Load $queryLimit entries from $queryOffset (range $itemsRangeStart:${itemsRangeStart + itemsRangeSize})")
        }

        currentItemsLoaderJob = GlobalScope.launch {
            val entries = Entries.query(queryCriteria, Entry.QueryHelper)

            if (isActive) {
                if (entries.isEmpty()) {
                    items.postValue(LoadedItems(0, 0, emptyArray()))
                    return@launch
                }

                val entriesIterator = entries.iterator()

                val itemsRange = mutableListOf<Item>()

                var prevEntry = entriesIterator.next()
                itemsRange.add(Header(prevEntry.numericDate, prevEntry.formattedDate))
                itemsRange.add(prevEntry)

                var index = queryCriteria.offset
                entriesIterator.forEach { entry ->
                    index += 1
                    if (entry.numericDate != prevEntry.numericDate) {
                        itemsRange.add(Header(entry.numericDate, entry.formattedDate))
                    } else {
                        itemsRange.add(FixedDivider(entry.numericDate, entry.formattedDate, index))
                    }
                    itemsRange.add(entry)
                    prevEntry = entry
                }

                if (isActive) {
                    items.postValue(LoadedItems(itemsCount, itemsRangeStart, itemsRange.toTypedArray()))
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

    interface Divider : Item

    class FixedDivider(override val numericDate: Int, override val formattedDate: String, index: Int) : Divider {
        override val id: Long = createPlaceholderId(index)
    }

    data class DividerPlaceholder(override var numericDate: Int = 0, override var formattedDate: String = "", internal var index: Int = 0) : Divider {
        override val id: Long
            get() = createPlaceholderId(index)
    }

    class Header(override val numericDate: Int, override val formattedDate: String) : Item {
        override val id: Long = -numericDate.toLong()
    }

    data class EntryPlaceholder(override var numericDate: Int = 0, override var formattedDate: String = "", internal var index: Int = 0) : Item {
        override val id: Long
            get() = createPlaceholderId(index)
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

        object QueryHelper : Entries.QueryHelper<Entry>(
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

    class LoadedItems(override val size: Int, private val rangeStart: Int, private val range: Array<Item>) : AbstractList<Item>() {
        private val topDividerPlaceholder: DividerPlaceholder
        private val topEntryPlaceholder: EntryPlaceholder
        private val bottomDividerPlaceholder: DividerPlaceholder
        private val bottomEntryPlaceholder: EntryPlaceholder

        init {
            if (range.isEmpty()) {
                topDividerPlaceholder = DividerPlaceholder()
                topEntryPlaceholder = EntryPlaceholder()
                bottomDividerPlaceholder = DividerPlaceholder()
                bottomEntryPlaceholder = EntryPlaceholder()
            } else {
                val firstItem = range[0]
                topDividerPlaceholder = DividerPlaceholder(numericDate = firstItem.numericDate, formattedDate = firstItem.formattedDate)
                topEntryPlaceholder = EntryPlaceholder(numericDate = firstItem.numericDate, formattedDate = firstItem.formattedDate)
                val lastItem = range[range.size - 1]
                bottomDividerPlaceholder = DividerPlaceholder(numericDate = lastItem.numericDate, formattedDate = lastItem.formattedDate)
                bottomEntryPlaceholder = EntryPlaceholder(numericDate = lastItem.numericDate, formattedDate = lastItem.formattedDate)
            }
        }

        override fun get(index: Int): Item = when {
            index < rangeStart -> when {
                index % 2 == 0 -> {
                    topDividerPlaceholder.index = index
                    topDividerPlaceholder
                }
                else -> {
                    topEntryPlaceholder.index = index
                    topEntryPlaceholder
                }
            }
            index >= rangeStart + range.size -> when {
                index % 2 == 0 -> {
                    bottomDividerPlaceholder.index = index
                    bottomDividerPlaceholder
                }
                else -> {
                    bottomEntryPlaceholder.index = index
                    bottomEntryPlaceholder
                }
            }
            else -> range[index - rangeStart]
        }

        fun getId(index: Int): Long = when {
            index < rangeStart || index >= rangeStart + range.size -> createPlaceholderId(index)
            else -> range[index - rangeStart].id
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

private fun createPlaceholderId(index: Int) = -4200_00_00L - index
