package com.tughi.aggregator.activities.reader

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.Repository

class ReaderActivityViewModel(queryCriteria: Entries.QueryCriteria) : ViewModel() {

    private val repository = Entries(
            object : Repository.Factory<Entry>() {
                override val columns = arrayOf(
                        Entries.ID,
                        Entries.PINNED_TIME,
                        Entries.READ_TIME
                )

                override fun create(cursor: Cursor) = Entry(
                        id = cursor.getLong(0),
                        pinnedTime = cursor.getLong(1),
                        readTime = cursor.getLong(2)
                )
            }
    )

    val entries: LiveData<List<Entry>> = repository.liveQuery(queryCriteria)

    data class Entry(
            val id: Long,
            val readTime: Long,
            val pinnedTime: Long
    )

    class Factory(private val queryCriteria: Entries.QueryCriteria) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderActivityViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderActivityViewModel(queryCriteria) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
