package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getLongOrNull
import com.tughi.aggregator.data.EntryTags

class EntryTag(
    val entryId: Long,
    val tagId: Long,
    val tagTime: Long,
    val entryTagRuleId: Long?,
) {
    object QueryHelper : EntryTags.QueryHelper<EntryTag>(
        EntryTags.ENTRY_ID,
        EntryTags.TAG_ID,
        EntryTags.TAG_TIME,
        EntryTags.ENTRY_TAG_RULE_ID,
    ) {
        override fun createRow(cursor: Cursor) = EntryTag(
            entryId = cursor.getLong(0),
            tagId = cursor.getLong(1),
            tagTime = cursor.getLong(2),
            entryTagRuleId = cursor.getLongOrNull(3),
        )
    }
}
