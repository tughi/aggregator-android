package com.tughi.aggregator.ion

import android.database.Cursor
import com.tughi.aggregator.data.MyFeedTags

class MyFeedTag(
    val tagId: Long,
    val type: Int,
) {
    object QueryHelper : MyFeedTags.QueryHelper<MyFeedTag>(
        MyFeedTags.TAG_ID,
        MyFeedTags.TYPE,
    ) {
        override fun createRow(cursor: Cursor) = MyFeedTag(
            tagId = cursor.getLong(0),
            type = cursor.getInt(1),
        )
    }
}
