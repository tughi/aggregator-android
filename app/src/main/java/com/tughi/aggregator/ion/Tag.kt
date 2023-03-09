package com.tughi.aggregator.ion

import android.database.Cursor
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.tughi.aggregator.data.Tags

private const val FIELD_ID = "id"
private const val FIELD_NAME = "name"
private const val FIELD_EDITABLE = "editable"

class Tag private constructor(structElement: StructElement, validate: Boolean) : CustomElement(structElement, validate), Tags.Insertable {
    constructor(structElement: StructElement) : this(structElement, validate = true)

    constructor(
        id: Long,
        name: String,
        editable: Boolean,
    ) : this(
        ionStructOf(
            listOf(
                field(FIELD_ID, ionInt(id)),
                field(FIELD_NAME, ionString(name)),
                field(FIELD_EDITABLE, ionBool(editable)),
            ),
            annotations = listOf(Tag::class.simpleName!!),
        ),
        validate = false,
    )

    override fun validate() {
        checkAnnotation(Tag::class.simpleName!!)
        checkField(FIELD_ID, ElementType.INT)
        checkField(FIELD_NAME, ElementType.STRING)
        checkField(FIELD_EDITABLE, ElementType.BOOL)
    }

    override fun insertData(): Array<Pair<Tags.TableColumn, Any?>> {
        val columns = mutableListOf<Pair<Tags.TableColumn, Any?>>()

        for (field in fields) {
            columns.add(
                when (field.name) {
                    FIELD_EDITABLE -> Tags.EDITABLE to if (field.value.booleanValue) 1 else 0
                    FIELD_ID -> Tags.ID to field.value.longValue
                    FIELD_NAME -> Tags.NAME to field.value.stringValue
                    else -> throw IllegalStateException("Unsupported field: ${field.name}")
                }
            )
        }

        return columns.toTypedArray()
    }

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
