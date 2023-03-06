package com.tughi.aggregator.ion

import android.database.Cursor
import com.tughi.aggregator.data.Tags

class Tag(
    val id: Long,
    val name: String,
    val editable: Boolean,
) {
    object QueryHelper : Tags.QueryHelper<Tag>(
        Tags.ID,
        Tags.NAME,
        Tags.EDITABLE,
    ) {
        override fun createRow(cursor: Cursor) = Tag(
            id = cursor.getLong(0),
            name = cursor.getString(1),
            editable = cursor.getInt(2) != 0,
        )
    }
}
