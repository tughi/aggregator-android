package com.tughi.aggregator.activities.reader

import android.database.Cursor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.Repository

internal class ReaderFragmentViewModel(entryId: Long, entryReadTime: Long) : ViewModel() {

    private val repository = Entries(
            object : Repository.Factory<Entry>() {
                override val columns = arrayOf(
                        Entries.ID,
                        Entries.TITLE,
                        Entries.LINK,
                        Entries.CONTENT,
                        Entries.AUTHOR,
                        Entries.PUBLISH_TIME,
                        Entries.FEED_TITLE,
                        Entries.FEED_LANGUAGE,
                        Entries.PINNED_TIME,
                        Entries.READ_TIME
                )

                override fun create(cursor: Cursor) = Entry(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        link = cursor.getString(2),
                        content = cursor.getString(3),
                        author = cursor.getString(4),
                        publishTime = cursor.getLong(5),
                        feedTitle = cursor.getString(6),
                        feedLanguage = cursor.getString(7),
                        readTime = cursor.getLong(8),
                        pinnedTime = cursor.getLong(9)
                )
            }
    )

    val entry = repository.liveQuery(entryId)

    data class Entry(
            val id: Long,
            val title: String?,
            val link: String?,
            val content: String?,
            val author: String?,
            val publishTime: Long,
            val feedTitle: String,
            val feedLanguage: String?,
            val readTime: Long,
            val pinnedTime: Long
    )

    class Factory(private val entryId: Long, private val entryReadTime: Long) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderFragmentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderFragmentViewModel(entryId, entryReadTime) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
