package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getLongOrNull
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonWriter
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

    fun writeTo(ionWriter: IonWriter, ionSystem: IonSystem) {
        ionSystem.newEmptyStruct().apply {
            setTypeAnnotations(EntryTagRule::class.simpleName)
            add(EntryTagRule::id.name, ionSystem.newInt(id))
            if (feedId != null) {
                add(EntryTagRule::feedId.name, ionSystem.newInt(feedId))
            }
            add(EntryTagRule::tagId.name, ionSystem.newInt(tagId))
            add(EntryTagRule::condition.name, ionSystem.newString(condition))
        }.writeTo(ionWriter)
    }
}

fun entryTagRuleData(ionStruct: IonStruct): Array<Pair<EntryTagRules.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<EntryTagRules.TableColumn, Any?>>()

    for (ionValue in ionStruct) {
        columns.add(
            when (ionValue.fieldName) {
                EntryTagRule::condition.name -> EntryTagRules.CONDITION to ionValue.stringValue()
                EntryTagRule::feedId.name -> EntryTagRules.FEED_ID to ionValue.longValue()
                EntryTagRule::id.name -> EntryTagRules.ID to ionValue.longValue()
                EntryTagRule::tagId.name -> EntryTagRules.TAG_ID to ionValue.longValue()
                else -> throw IllegalStateException("Unsupported field: ${ionValue.fieldName}")
            }
        )
    }

    return columns.toTypedArray()
}
