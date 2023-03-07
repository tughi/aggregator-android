package com.tughi.aggregator.ion

import android.database.Cursor
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonWriter
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

    fun writeTo(ionWriter: IonWriter, ionSystem: IonSystem) {
        ionSystem.newEmptyStruct().apply {
            setTypeAnnotations(MyFeedTag::class.simpleName)
            add(MyFeedTag::tagId.name, ionSystem.newInt(tagId))
            add(MyFeedTag::type.name, ionSystem.newInt(this@MyFeedTag.type))
        }.writeTo(ionWriter)
    }
}

fun myFeedTagData(ionStruct: IonStruct): Array<Pair<MyFeedTags.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<MyFeedTags.TableColumn, Any?>>()

    for (ionValue in ionStruct) {
        columns.add(
            when (ionValue.fieldName) {
                MyFeedTag::tagId.name -> MyFeedTags.TAG_ID to ionValue.longValue()
                MyFeedTag::type.name -> MyFeedTags.TYPE to ionValue.intValue()
                else -> throw IllegalStateException("Unsupported field: ${ionValue.fieldName}")
            }
        )
    }

    return columns.toTypedArray()
}
