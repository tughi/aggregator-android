package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getLongOrNull
import com.tughi.aggregator.data.EntryTagRules

class EntryTagRule(
    val id: Long,
    val feedId: Long?,
    val tagId: Long,
    val condition: String,
) {
    object QueryHelper : EntryTagRules.QueryHelper<EntryTagRule>(
        EntryTagRules.ID,
        EntryTagRules.FEED_ID,
        EntryTagRules.TAG_ID,
        EntryTagRules.CONDITION,
    ) {
        override fun createRow(cursor: Cursor) = EntryTagRule(
            id = cursor.getLong(0),
            feedId = cursor.getLongOrNull(1),
            tagId = cursor.getLong(2),
            condition = cursor.getString(3),
        )
    }
}
