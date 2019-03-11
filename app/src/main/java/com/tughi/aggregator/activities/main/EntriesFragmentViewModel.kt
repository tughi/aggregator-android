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
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.Repository
import com.tughi.aggregator.preferences.EntryListSettings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EntriesFragmentViewModel(initialQueryCriteria: Entries.QueryCriteria) : ViewModel() {

    private val sessionTime = System.currentTimeMillis()

    val queryCriteria = MutableLiveData<Entries.QueryCriteria>().apply {
        value = when (initialQueryCriteria) {
            is Entries.QueryCriteria.FeedEntries -> initialQueryCriteria.copy(sessionTime = sessionTime)
            is Entries.QueryCriteria.MyFeedEntries -> initialQueryCriteria.copy(sessionTime = sessionTime)
        }
    }

    private val entryFactory = object : Repository.Factory<Entry>() {
        private val context = App.instance

        override val columns = arrayOf(
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
        )

        override fun create(cursor: Cursor) = Entry(
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


    private val storedEntries = Transformations.switchMap(queryCriteria) { queryCriteria ->
        Entries.liveQuery(queryCriteria, entryFactory)
    }

    private val transformedEntries = MediatorLiveData<List<Entry>>().also {
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

    val entries: LiveData<List<Entry>>
        get() = transformedEntries

    fun changeSortOrder(sortOrder: Entries.SortOrder) {
        EntryListSettings.entriesSortOrder = sortOrder

        transformedEntries.value = null
        queryCriteria.value?.let { value ->
            queryCriteria.value = when (value) {
                is Entries.QueryCriteria.FeedEntries -> value.copy(sortOrder = sortOrder)
                is Entries.QueryCriteria.MyFeedEntries -> value.copy(sortOrder = sortOrder)
            }
        }
    }

    fun changeShowRead(showRead: Boolean) {
        transformedEntries.value = null
        queryCriteria.value?.let { value ->
            queryCriteria.value = when (value) {
                is Entries.QueryCriteria.FeedEntries -> value.copy(sessionTime = if (showRead) 0 else sessionTime)
                is Entries.QueryCriteria.MyFeedEntries -> value.copy(sessionTime = if (showRead) 0 else sessionTime)
            }
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
            val type: EntriesFragmentEntryType
    )

    class Factory(private val initialQueryCriteria: Entries.QueryCriteria) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EntriesFragmentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EntriesFragmentViewModel(initialQueryCriteria) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
