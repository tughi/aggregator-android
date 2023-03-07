package com.tughi.aggregator.ion

import android.database.Cursor
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonWriter
import com.tughi.aggregator.data.Entries

class Entry(
    val id: Long,
    val uid: String,
    val feedId: Long,
    val title: String?,
    val link: String?,
    val content: String?,
    val author: String?,
    val publishTime: Long?,
    val insertTime: Long,
    val updateTime: Long,
    val readTime: Long,
    val pinnedTime: Long,
    val starredTime: Long,
) {
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

    fun writeTo(ionWriter: IonWriter, ionSystem: IonSystem) {
        ionSystem.newEmptyStruct().apply {
            setTypeAnnotations(Entry::class.simpleName)
            add(Entry::id.name, ionSystem.newInt(id))
            add(Entry::uid.name, ionSystem.newString(uid))
            add(Entry::feedId.name, ionSystem.newInt(feedId))
            if (title != null) {
                add(Entry::title.name, ionSystem.newString(title))
            }
            if (link != null) {
                add(Entry::link.name, ionSystem.newString(link))
            }
            if (content != null) {
                add(Entry::content.name, ionSystem.newString(content))
            }
            if (author != null) {
                add(Entry::author.name, ionSystem.newString(author))
            }
            if (publishTime != null) {
                add(Entry::publishTime.name, ionSystem.newInt(publishTime))
            }
            add(Entry::insertTime.name, ionSystem.newInt(insertTime))
            add(Entry::updateTime.name, ionSystem.newInt(updateTime))
            add(Entry::readTime.name, ionSystem.newInt(readTime))
            add(Entry::pinnedTime.name, ionSystem.newInt(pinnedTime))
            add(Entry::starredTime.name, ionSystem.newInt(starredTime))
        }.writeTo(ionWriter)
    }
}

fun entryData(ionStruct: IonStruct): Array<Pair<Entries.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<Entries.TableColumn, Any?>>()

    for (ionValue in ionStruct) {
        columns.add(
            when (ionValue.fieldName) {
                Entry::author.name -> Entries.AUTHOR to ionValue.stringValue()
                Entry::content.name -> Entries.CONTENT to ionValue.stringValue()
                Entry::feedId.name -> Entries.FEED_ID to ionValue.longValue()
                Entry::id.name -> Entries.ID to ionValue.longValue()
                Entry::insertTime.name -> Entries.INSERT_TIME to ionValue.longValue()
                Entry::link.name -> Entries.LINK to ionValue.stringValue()
                Entry::pinnedTime.name -> Entries.PINNED_TIME to ionValue.longValue()
                Entry::publishTime.name -> Entries.PUBLISH_TIME to ionValue.longValue()
                Entry::readTime.name -> Entries.READ_TIME to ionValue.longValue()
                Entry::starredTime.name -> Entries.STARRED_TIME to ionValue.longValue()
                Entry::title.name -> Entries.TITLE to ionValue.stringValue()
                Entry::uid.name -> Entries.UID to ionValue.stringValue()
                Entry::updateTime.name -> Entries.UPDATE_TIME to ionValue.longValue()
                else -> throw IllegalStateException("Unsupported field: ${ionValue.fieldName}")
            }
        )
    }

    return columns.toTypedArray()
}
