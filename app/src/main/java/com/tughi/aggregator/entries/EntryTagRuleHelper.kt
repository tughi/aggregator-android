package com.tughi.aggregator.entries

import android.database.Cursor
import android.database.SQLException
import android.text.format.DateUtils
import androidx.collection.LongSparseArray
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.AllEntriesQueryCriteria
import com.tughi.aggregator.data.AllFeedEntriesQueryCriteria
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.EntryTagRuleQueryCriteria
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.EntryTags
import com.tughi.aggregator.entries.conditions.BooleanExpression
import com.tughi.aggregator.entries.conditions.Condition
import com.tughi.aggregator.entries.conditions.EmptyExpression
import com.tughi.aggregator.entries.conditions.Expression
import com.tughi.aggregator.entries.conditions.InvalidExpression
import com.tughi.aggregator.entries.conditions.PropertyExpression
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object EntryTagRuleHelper {

    private val activeJobs = LongSparseArray<Job>()

    fun apply(entryTagRuleId: Long, deleteOldTags: Boolean = false) {
        val job = contentScope.launch {
            val currentJob = coroutineContext[Job]

            val oldJob = synchronized(activeJobs) {
                val oldJob = activeJobs.get(entryTagRuleId)
                activeJobs.put(entryTagRuleId, currentJob)
                return@synchronized oldJob
            }

            oldJob?.cancelAndJoin()

            val entryTagRule = EntryTagRules.queryOne(EntryTagRuleQueryCriteria(entryTagRuleId), EntryTagRule.QueryHelper) ?: return@launch
            val entryTagTime = System.currentTimeMillis()

            if (deleteOldTags) {
                EntryTags.delete(EntryTags.DeleteRuleTagsCriteria(entryTagRule.id))
            }

            val entriesQueryCriteria = if (entryTagRule.feedId != null) AllFeedEntriesQueryCriteria(entryTagRule.feedId) else AllEntriesQueryCriteria()

            val matchedEntryIds = Longs(1000)

            Entries.query(entriesQueryCriteria, object : Entries.QueryHelper<Any>(Entries.ID, Entries.TITLE, Entries.LINK, Entries.CONTENT) {
                var lastCommitTime = entryTagTime

                override fun createRow(cursor: Cursor): Any {
                    if (!isActive) {
                        throw CancellationException()
                    }

                    val entryId = cursor.getLong(0)
                    val entryTitle = cursor.getString(1)
                    val entryLink = cursor.getString(2)
                    val entryContent = cursor.getString(3)

                    if (entryTagRule.matches(entryTitle, entryLink, entryContent)) {
                        val currentTime = System.currentTimeMillis()
                        if (matchedEntryIds.isFull() || currentTime - lastCommitTime > DateUtils.SECOND_IN_MILLIS) {
                            tagEntries(matchedEntryIds, entryTagRule, entryTagTime)
                            lastCommitTime = System.currentTimeMillis()
                        }

                        matchedEntryIds.add(entryId)
                    }

                    return Unit
                }
            })

            tagEntries(matchedEntryIds, entryTagRule, entryTagTime)
        }

        job.invokeOnCompletion {
            synchronized(activeJobs) { activeJobs.remove(entryTagRuleId, job) }
        }
    }

    private fun tagEntries(entryIds: Longs, entryTagRule: EntryTagRule, entryTagTime: Long) {
        val arraySize = entryIds.size
        if (arraySize > 0) {
            Database.transaction {
                val array = entryIds.array
                for (index in 0 until arraySize) {
                    try {
                        EntryTags.insert(
                            EntryTags.ENTRY_ID to array[index],
                            EntryTags.TAG_ID to entryTagRule.tagId,
                            EntryTags.TAG_TIME to entryTagTime,
                            EntryTags.ENTRY_TAG_RULE_ID to entryTagRule.id
                        )
                    } catch (_: SQLException) {
                        // ignored... probably, entry doesn't exist anymore
                    }
                }
            }
            entryIds.clear()
        }
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
    val condition: Condition,
    val feedId: Long?
) {
    fun matches(title: String?, link: String?, content: String?): Boolean {
        return condition.expression.matches(title, link, content)
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
            Condition(cursor.getString(2)),
            if (cursor.isNull(3)) null else cursor.getLong(3)
        )
    }
}

private fun Expression.matches(title: String?, link: String?, content: String?): Boolean = when (this) {
    EmptyExpression, InvalidExpression -> true // ignored
    is PropertyExpression -> {
        val property = when (property) {
            PropertyExpression.Property.TITLE -> title ?: ""
            PropertyExpression.Property.LINK -> link ?: ""
            PropertyExpression.Property.CONTENT -> content ?: ""
        }
        when (operator) {
            PropertyExpression.Operator.CONTAINS -> property.contains(value)
            PropertyExpression.Operator.ENDS_WITH -> property.endsWith(value)
            PropertyExpression.Operator.IS -> property.equals(value)
            PropertyExpression.Operator.STARTS_WITH -> property.startsWith(value)
        }
    }
    is BooleanExpression -> when (operator) {
        BooleanExpression.Operator.AND -> left.matches(title, link, content) && right.matches(title, link, content)
        BooleanExpression.Operator.OR -> left.matches(title, link, content) || right.matches(title, link, content)
    }
}
