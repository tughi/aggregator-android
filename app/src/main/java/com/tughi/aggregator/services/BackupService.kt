package com.tughi.aggregator.services

import android.app.Service
import android.content.Intent
import android.database.Cursor
import android.os.Binder
import android.os.IBinder
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.MutableLiveData
import com.amazon.ion.IonType
import com.amazon.ion.IonWriter
import com.amazon.ion.system.IonTextWriterBuilder
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.AllEntriesQueryCriteria
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.EntryTags
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.MyFeedTags
import com.tughi.aggregator.data.Tags
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.zip.GZIPOutputStream


class BackupService : Service() {
    private val binder = LocalBinder()

    private var currentJob: Job? = null

    val status = MutableLiveData(Status(false))

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val job = contentScope.launch {
            intent?.data?.let { uri ->
                contentResolver.openOutputStream(uri)?.use {
                    GZIPOutputStream(it).use { output ->
                        backup(output)
                    }
                }
            }
        }

        job.invokeOnCompletion {
            currentJob = null
            status.postValue(Status(false))
        }

        currentJob = job

        return START_NOT_STICKY
    }

    private fun backup(output: OutputStream) {
        status.postValue(Status(true))

        val ionWriter = IonTextWriterBuilder.pretty().build(output)

        ionWriter.writeAggregatorData()

        Database.transaction {
            val feedsCount = Feeds.queryCount(Feeds.AllCriteria, Feed.QueryHelper)
            ionWriter.writeTypedListSize(Feeds::class.simpleName!!, feedsCount)

            val entriesCount = Entries.queryCount(AllEntriesQueryCriteria(), Entry.QueryHelper)
            ionWriter.writeTypedListSize(Entries::class.simpleName!!, entriesCount)

            val tagsCount = Tags.queryCount(Tags.QueryAllTagsCriteria, Tag.QueryHelper)
            ionWriter.writeTypedListSize(Tags::class.simpleName!!, tagsCount)

            val entryTagRulesCount = EntryTagRules.queryCount(EntryTagRules.QueryAllCriteria, EntryTagRule.QueryHelper)
            ionWriter.writeTypedListSize(EntryTagRules::class.simpleName!!, entryTagRulesCount)

            val entryTagsCount = EntryTags.queryCount(EntryTags.QueryAllCriteria, EntryTag.QueryHelper)
            ionWriter.writeTypedListSize(EntryTags::class.simpleName!!, entryTagsCount)

            val myFeedTagsCount = MyFeedTags.queryCount(MyFeedTags.QueryAllCriteria, MyFeedTag.QueryHelper)
            ionWriter.writeTypedListSize(MyFeedTags::class.simpleName!!, myFeedTagsCount)

            val totalRows = (feedsCount + entriesCount + tagsCount + entryTagRulesCount + entryTagsCount + myFeedTagsCount).toFloat()
            var processedRows = 0
            fun updateStatus() {
                if (currentJob!!.isCancelled) {
                    throw CancellationException("Cancelled")
                }

                processedRows += 1
                status.postValue(Status(true, processedRows / totalRows))
            }

            ionWriter.writeTypedList(Feeds::class.simpleName!!, feedsCount) {
                Feeds.forEach(Feeds.AllCriteria, Feed.QueryHelper) { feed ->
                    ionWriter.writeFeed(feed)
                    updateStatus()
                }
            }

            ionWriter.writeTypedList(Entries::class.simpleName!!, entriesCount) {
                Entries.forEach(AllEntriesQueryCriteria(), Entry.QueryHelper) { entry ->
                    ionWriter.writeEntry(entry)
                    updateStatus()
                }
            }

            ionWriter.writeTypedList(Tags::class.simpleName!!, tagsCount) {
                Tags.forEach(Tags.QueryAllTagsCriteria, Tag.QueryHelper) { tag ->
                    ionWriter.writeTag(tag)
                    updateStatus()
                }
            }

            ionWriter.writeTypedList(EntryTagRules::class.simpleName!!, entryTagRulesCount) {
                EntryTagRules.forEach(EntryTagRules.QueryAllCriteria, EntryTagRule.QueryHelper) { entryTagRule ->
                    ionWriter.writeEntryTagRule(entryTagRule)
                    updateStatus()
                }
            }

            ionWriter.writeTypedList(EntryTags::class.simpleName!!, entryTagsCount) {
                EntryTags.forEach(EntryTags.QueryAllCriteria, EntryTag.QueryHelper) { entryTag ->
                    ionWriter.writeEntryTag(entryTag)
                    updateStatus()
                }
            }

            ionWriter.writeTypedList(MyFeedTags::class.simpleName!!, myFeedTagsCount) {
                MyFeedTags.forEach(MyFeedTags.QueryAllCriteria, MyFeedTag.QueryHelper) { myFeedTag ->
                    ionWriter.writeMyFeedTag(myFeedTag)
                    updateStatus()
                }
            }
        }

        ionWriter.finish()
    }

    fun cancel() {
        currentJob?.cancel()
    }

    private fun IonWriter.writeAggregatorData() {
        setTypeAnnotations("AggregatorData")
        stepIn(IonType.STRUCT)

        setFieldName("version")
        writeInt(1)

        stepOut()
    }

    private fun IonWriter.writeTypedList(listType: String, listSize: Int, writeListItems: () -> Unit) {
        if (listSize > 0) {
            setTypeAnnotations(listType)
            stepIn(IonType.LIST)

            writeListItems()

            stepOut()
        }
    }

    private fun IonWriter.writeTypedListSize(listType: String, listSize: Int) {
        setTypeAnnotations(listType + "Size")
        writeInt(listSize.toLong())
    }

    private fun IonWriter.writeEntry(entry: Entry) {
        stepIn(IonType.STRUCT)

        setFieldName(entry::id.name)
        writeInt(entry.id)

        setFieldName(entry::uid.name)
        writeString(entry.uid)

        setFieldName(entry::feedId.name)
        writeInt(entry.feedId)

        if (entry.title != null) {
            setFieldName(entry::title.name)
            writeString(entry.title)
        }

        if (entry.link != null) {
            setFieldName(entry::link.name)
            writeString(entry.link)
        }

        if (entry.content != null) {
            setFieldName(entry::content.name)
            writeString(entry.content)
        }

        if (entry.author != null) {
            setFieldName(entry::author.name)
            writeString(entry.author)
        }

        if (entry.publishTime != null) {
            setFieldName(entry::publishTime.name)
            writeInt(entry.publishTime)
        }

        setFieldName(entry::insertTime.name)
        writeInt(entry.insertTime)

        setFieldName(entry::updateTime.name)
        writeInt(entry.updateTime)

        setFieldName(entry::readTime.name)
        writeInt(entry.readTime)

        setFieldName(entry::pinnedTime.name)
        writeInt(entry.pinnedTime)

        setFieldName(entry::starredTime.name)
        writeInt(entry.starredTime)

        stepOut()
    }

    private fun IonWriter.writeEntryTag(entryTag: EntryTag) {
        stepIn(IonType.STRUCT)

        setFieldName(entryTag::entryId.name)
        writeInt(entryTag.entryId)

        setFieldName(entryTag::tagId.name)
        writeInt(entryTag.tagId)

        setFieldName(entryTag::tagTime.name)
        writeInt(entryTag.tagTime)

        if (entryTag.entryTagRuleId != null) {
            setFieldName(entryTag::entryTagRuleId.name)
            writeInt(entryTag.entryTagRuleId)
        }

        stepOut()
    }

    private fun IonWriter.writeEntryTagRule(entryTagRule: EntryTagRule) {
        stepIn(IonType.STRUCT)

        setFieldName(entryTagRule::id.name)
        writeInt(entryTagRule.id)

        if (entryTagRule.feedId != null) {
            setFieldName(entryTagRule::feedId.name)
            writeInt(entryTagRule.feedId)
        }

        setFieldName(entryTagRule::tagId.name)
        writeInt(entryTagRule.tagId)

        setFieldName(entryTagRule::condition.name)
        writeString(entryTagRule.condition)

        stepOut()
    }

    private fun IonWriter.writeFeed(feed: Feed) {
        stepIn(IonType.STRUCT)

        setFieldName(feed::id.name)
        writeInt(feed.id)

        setFieldName(feed::url.name)
        writeString(feed.url)

        setFieldName(feed::title.name)
        writeString(feed.title)

        if (feed.customTitle != null) {
            setFieldName(feed::customTitle.name)
            writeString(feed.customTitle)
        }

        if (feed.link != null) {
            setFieldName(feed::link.name)
            writeString(feed.link)
        }

        if (feed.language != null) {
            setFieldName(feed::language.name)
            writeString(feed.language)
        }

        if (feed.faviconUrl != null) {
            setFieldName(feed::faviconUrl.name)
            writeString(feed.faviconUrl)
        }

        setFieldName(feed::cleanupMode.name)
        writeString(feed.cleanupMode)

        setFieldName(feed::updateMode.name)
        writeString(feed.updateMode)

        setFieldName(feed::lastUpdateTime.name)
        writeInt(feed.lastUpdateTime)

        if (feed.lastUpdateError != null) {
            setFieldName(feed::lastUpdateError.name)
            writeString(feed.lastUpdateError)
        }

        setFieldName(feed::nextUpdateTime.name)
        writeInt(feed.nextUpdateTime)

        setFieldName(feed::nextUpdateRetry.name)
        writeInt(feed.nextUpdateRetry.toLong())

        if (feed.httpEtag != null) {
            setFieldName(feed::httpEtag.name)
            writeString(feed.httpEtag)
        }

        if (feed.httpLastModified != null) {
            setFieldName(feed::httpLastModified.name)
            writeString(feed.httpLastModified)
        }

        stepOut()
    }

    private fun IonWriter.writeMyFeedTag(myFeedTag: MyFeedTag) {
        stepIn(IonType.STRUCT)

        setFieldName(myFeedTag::tagId.name)
        writeInt(myFeedTag.tagId)

        setFieldName(myFeedTag::type.name)
        writeInt(myFeedTag.type.toLong())

        stepOut()
    }

    private fun IonWriter.writeTag(tag: Tag) {
        stepIn(IonType.STRUCT)

        setFieldName(tag::id.name)
        writeInt(tag.id)

        setFieldName(tag::name.name)
        writeString(tag.name)

        setFieldName(tag::editable.name)
        writeBool(tag.editable)

        stepOut()
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@BackupService
    }

    data class Status(val busy: Boolean, val progress: Float = 0f)

    private class Entry(
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
    }

    private class EntryTag(
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
    }

    private class EntryTagRule(
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
    }

    private class Feed(
        val id: Long,
        val url: String,
        val title: String,
        val customTitle: String?,
        val link: String?,
        val language: String?,
        val faviconUrl: String?,
        val cleanupMode: String,
        val updateMode: String,
        val lastUpdateTime: Long,
        val lastUpdateError: String?,
        val nextUpdateTime: Long,
        val nextUpdateRetry: Int,
        val httpEtag: String?,
        val httpLastModified: String?,
    ) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
            Feeds.ID,
            Feeds.URL,
            Feeds.TITLE,
            Feeds.CUSTOM_TITLE,
            Feeds.LINK,
            Feeds.LANGUAGE,
            Feeds.FAVICON_URL,
            Feeds.CLEANUP_MODE,
            Feeds.UPDATE_MODE,
            Feeds.LAST_UPDATE_TIME,
            Feeds.LAST_UPDATE_ERROR,
            Feeds.NEXT_UPDATE_TIME,
            Feeds.NEXT_UPDATE_RETRY,
            Feeds.HTTP_ETAG,
            Feeds.HTTP_LAST_MODIFIED,
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                id = cursor.getLong(0),
                url = cursor.getString(1),
                title = cursor.getString(2),
                customTitle = cursor.getStringOrNull(3),
                link = cursor.getStringOrNull(4),
                language = cursor.getStringOrNull(5),
                faviconUrl = cursor.getStringOrNull(6),
                cleanupMode = cursor.getString(7),
                updateMode = cursor.getString(8),
                lastUpdateTime = cursor.getLong(9),
                lastUpdateError = cursor.getStringOrNull(10),
                nextUpdateTime = cursor.getLong(11),
                nextUpdateRetry = cursor.getInt(12),
                httpEtag = cursor.getStringOrNull(13),
                httpLastModified = cursor.getStringOrNull(14),
            )
        }
    }

    private class MyFeedTag(
        val tagId: Long,
        val type: Int,
    ) {
        object QueryHelper : MyFeedTags.QueryHelper<MyFeedTag>(
            MyFeedTags.TAG_ID,
            MyFeedTags.TYPE,
        ) {
            override fun createRow(cursor: Cursor) = MyFeedTag(
                tagId = cursor.getLong(0),
                type = cursor.getInt(1),
            )
        }
    }

    private class Tag(
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
    }
}
