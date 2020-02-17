package com.tughi.aggregator.services

import android.database.Cursor
import com.tughi.aggregator.data.AllEntriesQueryCriteria
import com.tughi.aggregator.data.AllFeedEntriesQueryCriteria
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.EntryTagRuleQueryCriteria
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.EntryTags
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object EntryTagRuleHelper {

    fun apply(entryTagRuleId: Long, deleteOldTags: Boolean = false) {
        GlobalScope.launch {
            val entryTagRule = EntryTagRules.queryOne(EntryTagRuleQueryCriteria(entryTagRuleId), EntryTagRule.QueryHelper) ?: return@launch
            val entryTagTime = System.currentTimeMillis()

            if (deleteOldTags) {
                EntryTags.delete(EntryTags.DeleteRuleTagsCriteria(entryTagRule.id))
            }

            val entriesQueryCriteria = if (entryTagRule.feedId != null) AllFeedEntriesQueryCriteria(entryTagRule.feedId) else AllEntriesQueryCriteria()

            val matchedEntryIds = Longs(100)

            Entries.query(entriesQueryCriteria, object : Entries.QueryHelper<Any>(Entries.ID, Entries.TITLE) {
                override fun createRow(cursor: Cursor): Any {
                    val entryId = cursor.getLong(0)
                    val entryTitle = cursor.getString(1)

                    if (entryTagRule.matches(entryTitle, null, null)) {
                        if (matchedEntryIds.isFull()) {
                            tagEntries(matchedEntryIds, entryTagRule, entryTagTime)
                        }

                        matchedEntryIds.add(entryId)
                    }

                    return Unit
                }
            })

            tagEntries(matchedEntryIds, entryTagRule, entryTagTime)
        }
    }

    private fun tagEntries(entryIds: Longs, entryTagRule: EntryTagRule, entryTagTime: Long) {
        Database.transaction {
            val array = entryIds.array
            val arraySize = entryIds.size
            for (index in 0 until arraySize) {
                EntryTags.insert(
                        EntryTags.ENTRY_ID to array[index],
                        EntryTags.TAG_ID to entryTagRule.tagId,
                        EntryTags.TAG_TIME to entryTagTime,
                        EntryTags.ENTRY_TAG_RULE_ID to entryTagRule.id
                )
            }
        }
        entryIds.clear()
    }

    internal class Longs(private val capacity: Int) {
        val array = LongArray(capacity)
        var size = 0
            private set

        fun isFull(): Boolean = size == capacity

        fun add(value: Long) {
            array[size++] = value
        }

        fun clear() {
            size = 0
        }
    }

}

class EntryTagRule(
        val id: Long,
        val tagId: Long,
        val condition: String,
        val feedId: Long?
) {
    fun matches(title: String?, link: String?, content: String?): Boolean {
        // TODO: use condition
        return true
    }

    object QueryHelper : EntryTagRules.QueryHelper<EntryTagRule>(
            EntryTagRules.ID,
            EntryTagRules.TAG_ID,
            EntryTagRules.CONDITION,
            EntryTagRules.FEED_ID
    ) {
        override fun createRow(cursor: Cursor): EntryTagRule = EntryTagRule(
                cursor.getLong(0),
                cursor.getLong(1),
                cursor.getString(2),
                if (cursor.isNull(3)) null else cursor.getLong(3)
        )
    }
}
