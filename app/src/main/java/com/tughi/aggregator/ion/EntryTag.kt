package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getLongOrNull
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.StructField
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionStructOf
import com.tughi.aggregator.data.EntryTags

private const val FIELD_ENTRY_ID = "entryId"
private const val FIELD_TAG_ID = "tagId"
private const val FIELD_TAG_TIME = "tagTime"
private const val FIELD_ENTRY_TAG_RULE_ID = "entryTagRuleId"

class EntryTag private constructor(structElement: StructElement, validate: Boolean) : CustomElement(structElement, validate), EntryTags.Insertable {
    constructor(structElement: StructElement) : this(structElement, validate = true)

    constructor(
        entryId: Long,
        tagId: Long,
        tagTime: Long,
        entryTagRuleId: Long?,
    ) : this(
        ionStructOf(
            mutableListOf<StructField>().apply {
                add(field(FIELD_ENTRY_ID, ionInt(entryId)))
                add(field(FIELD_TAG_ID, ionInt(tagId)))
                add(field(FIELD_TAG_TIME, ionInt(tagTime)))
                if (entryTagRuleId != null) {
                    add(field(FIELD_ENTRY_TAG_RULE_ID, ionInt(entryTagRuleId)))
                }
            },
            annotations = listOf(EntryTag::class.simpleName!!),
        ),
        validate = false,
    )

    override fun validate() {
        checkAnnotation(EntryTag::class.simpleName!!)
        checkField(FIELD_ENTRY_ID, ElementType.INT)
        checkField(FIELD_TAG_ID, ElementType.INT)
        checkField(FIELD_TAG_TIME, ElementType.INT)
        checkOptionalField(FIELD_ENTRY_TAG_RULE_ID, ElementType.INT)
    }


    override fun insertData(): Array<Pair<EntryTags.TableColumn, Any?>> {
        val columns = mutableListOf<Pair<EntryTags.TableColumn, Any?>>()

        for (field in fields) {
            columns.add(
                when (field.name) {
                    FIELD_ENTRY_ID -> EntryTags.ENTRY_ID to field.value.longValue
                    FIELD_ENTRY_TAG_RULE_ID -> EntryTags.ENTRY_TAG_RULE_ID to field.value.longValue
                    FIELD_TAG_ID -> EntryTags.TAG_ID to field.value.longValue
                    FIELD_TAG_TIME -> EntryTags.TAG_TIME to field.value.longValue
                    else -> throw IllegalStateException("Unsupported field: ${field.name}")
                }
            )
        }

        return columns.toTypedArray()
    }

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
