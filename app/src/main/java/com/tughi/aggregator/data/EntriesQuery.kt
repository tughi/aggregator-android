package com.tughi.aggregator.data

import java.io.Serializable

sealed class EntriesQuery : Serializable {
    abstract val sortOrder: EntriesSortOrder
    abstract val showRead: Boolean
}

data class FeedEntriesQuery(val feedId: Long, val since: Long, override val sortOrder: EntriesSortOrder, override val showRead: Boolean = false) : EntriesQuery()

data class MyFeedEntriesQuery(val since: Long, override val sortOrder: EntriesSortOrder, override val showRead: Boolean = false) : EntriesQuery()
