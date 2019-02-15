package com.tughi.aggregator.data

import java.io.Serializable

sealed class EntriesQuery : Serializable {
    abstract val since: Long
    abstract val sortOrder: EntriesSortOrder
}

data class FeedEntriesQuery(val feedId: Long, override val since: Long, override val sortOrder: EntriesSortOrder) : EntriesQuery()

data class MyFeedEntriesQuery(override val since: Long, override val sortOrder: EntriesSortOrder) : EntriesQuery()
