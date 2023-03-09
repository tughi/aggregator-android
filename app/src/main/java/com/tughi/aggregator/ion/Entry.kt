package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.StructField
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.tughi.aggregator.data.Entries

private const val FIELD_ID = "id"
private const val FIELD_UID = "uid"
private const val FIELD_FEED_ID = "feedId"
private const val FIELD_TITLE = "title"
private const val FIELD_LINK = "link"
private const val FIELD_CONTENT = "content"
private const val FIELD_AUTHOR = "author"
private const val FIELD_PUBLISH_TIME = "publishTime"
private const val FIELD_INSERT_TIME = "insertTime"
private const val FIELD_UPDATE_TIME = "updateTime"
private const val FIELD_READ_TIME = "readTime"
private const val FIELD_PINNED_TIME = "pinnedTime"
private const val FIELD_STARRED_TIME = "starredTime"

class Entry private constructor(structElement: StructElement, validate: Boolean) : CustomElement(structElement, validate), Entries.Insertable {
    constructor(structElement: StructElement) : this(structElement, validate = true)

    constructor(
        id: Long,
        uid: String,
        feedId: Long,
        title: String?,
        link: String?,
        content: String?,
        author: String?,
        publishTime: Long?,
        insertTime: Long,
        updateTime: Long,
        readTime: Long,
        pinnedTime: Long,
        starredTime: Long,
    ) : this(
        ionStructOf(
            mutableListOf<StructField>().apply {
                add(field(FIELD_ID, ionInt(id)))
                add(field(FIELD_UID, ionString(uid)))
                add(field(FIELD_FEED_ID, ionInt(feedId)))
                if (title != null) {
                    add(field(FIELD_TITLE, ionString(title)))
                }
                if (link != null) {
                    add(field(FIELD_LINK, ionString(link)))
                }
                if (content != null) {
                    add(field(FIELD_CONTENT, ionString(content)))
                }
                if (author != null) {
                    add(field(FIELD_AUTHOR, ionString(author)))
                }
                if (publishTime != null) {
                    add(field(FIELD_PUBLISH_TIME, ionInt(publishTime)))
                }
                add(field(FIELD_INSERT_TIME, ionInt(insertTime)))
                add(field(FIELD_UPDATE_TIME, ionInt(updateTime)))
                add(field(FIELD_READ_TIME, ionInt(readTime)))
                add(field(FIELD_PINNED_TIME, ionInt(pinnedTime)))
                add(field(FIELD_STARRED_TIME, ionInt(starredTime)))
            },
            annotations = listOf(Entry::class.simpleName!!),
        ),
        validate = false,
    )

    override fun validate() {
        checkAnnotation(Entry::class.simpleName!!)
        checkField(FIELD_ID, ElementType.INT)
        checkField(FIELD_UID, ElementType.STRING)
        checkField(FIELD_FEED_ID, ElementType.INT)
        checkOptionalField(FIELD_TITLE, ElementType.STRING)
        checkOptionalField(FIELD_LINK, ElementType.STRING)
        checkOptionalField(FIELD_CONTENT, ElementType.STRING)
        checkOptionalField(FIELD_AUTHOR, ElementType.STRING)
        checkOptionalField(FIELD_PUBLISH_TIME, ElementType.INT)
        checkField(FIELD_INSERT_TIME, ElementType.INT)
        checkField(FIELD_UPDATE_TIME, ElementType.INT)
        checkField(FIELD_READ_TIME, ElementType.INT)
        checkField(FIELD_PINNED_TIME, ElementType.INT)
        checkField(FIELD_STARRED_TIME, ElementType.INT)
    }

    override fun insertData(): Array<Pair<Entries.TableColumn, Any?>> {
        val columns = mutableListOf<Pair<Entries.TableColumn, Any?>>()

        for (field in fields) {
            columns.add(
                when (field.name) {
                    FIELD_AUTHOR -> Entries.AUTHOR to field.value.stringValue
                    FIELD_CONTENT -> Entries.CONTENT to field.value.stringValue
                    FIELD_FEED_ID -> Entries.FEED_ID to field.value.longValue
                    FIELD_ID -> Entries.ID to field.value.longValue
                    FIELD_INSERT_TIME -> Entries.INSERT_TIME to field.value.longValue
                    FIELD_LINK -> Entries.LINK to field.value.stringValue
                    FIELD_PINNED_TIME -> Entries.PINNED_TIME to field.value.longValue
                    FIELD_PUBLISH_TIME -> Entries.PUBLISH_TIME to field.value.longValue
                    FIELD_READ_TIME -> Entries.READ_TIME to field.value.longValue
                    FIELD_STARRED_TIME -> Entries.STARRED_TIME to field.value.longValue
                    FIELD_TITLE -> Entries.TITLE to field.value.stringValue
                    FIELD_UID -> Entries.UID to field.value.stringValue
                    FIELD_UPDATE_TIME -> Entries.UPDATE_TIME to field.value.longValue
                    else -> throw IllegalStateException("Unsupported field: ${field.name}")
                }
            )
        }

        return columns.toTypedArray()
    }

    object QueryHelper : Entries.QueryHelper<Entry>(
        Entries.ID,
        Entries.UID,
        Entries.FEED_ID,
        Entries.TITLE,
        Entries.LINK,
        Entries.CONTENT,
        Entries.AUTHOR,
        Entries.PUBLISH_TIME,
        Entries.INSERT_TIME,
        Entries.UPDATE_TIME,
        Entries.READ_TIME,
        Entries.PINNED_TIME,
        Entries.STARRED_TIME,
    ) {
        override fun createRow(cursor: Cursor) = Entry(
            id = cursor.getLong(0),
            uid = cursor.getString(1),
            feedId = cursor.getLong(2),
            title = cursor.getStringOrNull(3),
            link = cursor.getStringOrNull(4),
            content = cursor.getStringOrNull(5),
            author = cursor.getStringOrNull(6),
            publishTime = cursor.getLongOrNull(7),
            insertTime = cursor.getLong(8),
            updateTime = cursor.getLong(9),
            readTime = cursor.getLong(10),
            pinnedTime = cursor.getLong(11),
            starredTime = cursor.getLong(12),
        )
    }
}
