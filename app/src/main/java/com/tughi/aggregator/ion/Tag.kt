package com.tughi.aggregator.ion

import android.database.Cursor
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonWriter
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

    fun writeTo(ionWriter: IonWriter, ionSystem: IonSystem) {
        ionSystem.newEmptyStruct().apply {
            setTypeAnnotations(Tag::class.simpleName)
            add(Tag::id.name, ionSystem.newInt(id))
            add(Tag::name.name, ionSystem.newString(name))
            add(Tag::editable.name, ionSystem.newBool(editable))
        }.writeTo(ionWriter)
    }
}

fun tagData(ionStruct: IonStruct): Array<Pair<Tags.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<Tags.TableColumn, Any?>>()

    for (ionValue in ionStruct) {
        columns.add(
            when (ionValue.fieldName) {
                Tag::editable.name -> Tags.EDITABLE to if (ionValue.booleanValue()) 1 else 0
                Tag::id.name -> Tags.ID to ionValue.longValue()
                Tag::name.name -> Tags.NAME to ionValue.stringValue()
                else -> throw IllegalStateException("Unsupported field: ${ionValue.fieldName}")
            }
        )
    }

    return columns.toTypedArray()
}
