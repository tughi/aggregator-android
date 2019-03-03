package com.tughi.aggregator.data

import com.tughi.aggregator.preferences.EntryListSettings
import java.io.Serializable

sealed class EntriesQuery : Serializable {
    abstract val sessionTime: Long
    abstract val sortOrder: EntriesSortOrder
}

data class FeedEntriesQuery(val feedId: Long, override val sessionTime: Long = 0, override val sortOrder: EntriesSortOrder = EntryListSettings.entriesSortOrder) : EntriesQuery()

data class MyFeedEntriesQuery(override val sessionTime: Long = 0, override val sortOrder: EntriesSortOrder = EntryListSettings.entriesSortOrder) : EntriesQuery()
