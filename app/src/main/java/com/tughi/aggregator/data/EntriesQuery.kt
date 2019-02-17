package com.tughi.aggregator.data

import java.io.Serializable

sealed class EntriesQuery : Serializable {
    abstract val sortOrder: EntriesSortOrder
}

data class FeedEntriesQuery(val feedId: Long, val since: Long, override val sortOrder: EntriesSortOrder) : EntriesQuery()

data class MyFeedEntriesQuery(val since: Long, override val sortOrder: EntriesSortOrder) : EntriesQuery()
