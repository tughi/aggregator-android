package com.tughi.aggregator.data

import java.io.Serializable

sealed class EntriesQuery : Serializable

data class FeedEntriesQuery(val feedId: Long, val since: Long) : EntriesQuery()

data class MyFeedEntriesQuery(val since: Long) : EntriesQuery()
