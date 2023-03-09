package com.tughi.aggregator.ion

import android.database.Cursor
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionStructOf
import com.tughi.aggregator.data.MyFeedTags

private const val FIELD_TAG_ID = "tagId"
private const val FIELD_TYPE = "type"

class MyFeedTag private constructor(structElement: StructElement, validate: Boolean) : CustomElement(structElement, validate), MyFeedTags.Insertable {
    constructor(structElement: StructElement) : this(structElement, validate = true)

    constructor(
        tagId: Long,
        type: Int,
    ) : this(
        ionStructOf(
            listOf(
                field(FIELD_TAG_ID, ionInt(tagId)),
                field(FIELD_TYPE, ionInt(type.toLong())),
            ),
            annotations = listOf(MyFeedTag::class.simpleName!!),
        ),
        validate = false,
    )

    override fun validate() {
        checkAnnotation(MyFeedTag::class.simpleName!!)
        checkField(FIELD_TAG_ID, ElementType.INT)
        checkField(FIELD_TYPE, ElementType.INT)
    }

    override fun insertData(): Array<Pair<MyFeedTags.TableColumn, Any?>> {
        val columns = mutableListOf<Pair<MyFeedTags.TableColumn, Any?>>()

        for (field in fields) {
            columns.add(
                when (field.name) {
                    FIELD_TAG_ID -> MyFeedTags.TAG_ID to field.value.longValue
                    FIELD_TYPE -> MyFeedTags.TYPE to field.value.longValue.toInt()
                    else -> throw IllegalStateException("Unsupported field: ${field.name}")
                }
            )
        }

        return columns.toTypedArray()
    }

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

