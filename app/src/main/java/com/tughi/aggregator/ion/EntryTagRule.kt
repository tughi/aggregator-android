package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getLongOrNull
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.StructField
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.tughi.aggregator.data.EntryTagRules

private const val FIELD_ID = "id"
private const val FIELD_FEED_ID = "feedId"
private const val FIELD_TAG_ID = "tagId"
private const val FIELD_CONDITION = "condition"

class EntryTagRule private constructor(structElement: StructElement, validate: Boolean) : CustomElement(structElement, validate), EntryTagRules.Insertable {
    constructor(structElement: StructElement) : this(structElement, validate = true)

    constructor(
        id: Long,
        feedId: Long?,
        tagId: Long,
        condition: String,
    ) : this(
        ionStructOf(
            mutableListOf<StructField>().apply {
                add(field(FIELD_ID, ionInt(id)))
                if (feedId != null) {
                    add(field(FIELD_FEED_ID, ionInt(feedId)))
                }
                add(field(FIELD_TAG_ID, ionInt(tagId)))
                add(field(FIELD_CONDITION, ionString(condition)))
            },
            annotations = listOf(EntryTagRule::class.simpleName!!),
        ),
        validate = false,
    )

    override fun validate() {
        checkAnnotation(EntryTagRule::class.simpleName!!)
        checkField(FIELD_ID, ElementType.INT)
        checkOptionalField(FIELD_FEED_ID, ElementType.INT)
        checkField(FIELD_TAG_ID, ElementType.INT)
        checkField(FIELD_CONDITION, ElementType.STRING)
    }


    override fun insertData(): Array<Pair<EntryTagRules.TableColumn, Any?>> {
        val columns = mutableListOf<Pair<EntryTagRules.TableColumn, Any?>>()

        for (field in fields) {
            columns.add(
                when (field.name) {
                    FIELD_CONDITION -> EntryTagRules.CONDITION to field.value.stringValue
                    FIELD_FEED_ID -> EntryTagRules.FEED_ID to field.value.longValue
                    FIELD_ID -> EntryTagRules.ID to field.value.longValue
                    FIELD_TAG_ID -> EntryTagRules.TAG_ID to field.value.longValue
                    else -> throw IllegalStateException("Unsupported field: ${field.name}")
                }
            )
        }

        return columns.toTypedArray()
    }

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
