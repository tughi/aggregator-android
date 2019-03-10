package com.tughi.aggregator.feeds

import com.tughi.aggregator.data.UpdateMode

data class OpmlFeed(
        val url: String,
        val title: String,
        val link: String?,
        val customTitle: String?,
        val category: String? = null,
        val updateMode: UpdateMode,
        val excluded: Boolean = false
)
