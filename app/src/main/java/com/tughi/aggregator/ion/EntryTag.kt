package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getLongOrNull
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonWriter
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

    fun writeTo(ionWriter: IonWriter, ionSystem: IonSystem) {
        ionSystem.newEmptyStruct().apply {
            setTypeAnnotations(EntryTag::class.simpleName)
            add(EntryTag::entryId.name, ionSystem.newInt(entryId))
            add(EntryTag::tagId.name, ionSystem.newInt(tagId))
            add(EntryTag::tagTime.name, ionSystem.newInt(tagTime))
            if (entryTagRuleId != null) {
                add(EntryTag::entryTagRuleId.name, ionSystem.newInt(entryTagRuleId))
            }
        }.writeTo(ionWriter)
    }
}

fun entryTagData(ionStruct: IonStruct): Array<Pair<EntryTags.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<EntryTags.TableColumn, Any?>>()

    for (ionValue in ionStruct) {
        columns.add(
            when (ionValue.fieldName) {
                EntryTag::entryId.name -> EntryTags.ENTRY_ID to ionValue.longValue()
                EntryTag::entryTagRuleId.name -> EntryTags.ENTRY_TAG_RULE_ID to ionValue.longValue()
                EntryTag::tagId.name -> EntryTags.TAG_ID to ionValue.longValue()
                EntryTag::tagTime.name -> EntryTags.TAG_TIME to ionValue.longValue()
                else -> throw IllegalStateException("Unsupported field: ${ionValue.fieldName}")
            }
        )
    }

    return columns.toTypedArray()
}
