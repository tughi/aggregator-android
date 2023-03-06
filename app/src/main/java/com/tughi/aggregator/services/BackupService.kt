package com.tughi.aggregator.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import com.amazon.ion.IonType
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.tughi.aggregator.BuildConfig
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.AllEntriesQueryCriteria
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.EntryTags
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.MyFeedTags
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.ion.AggregatorData
import com.tughi.aggregator.ion.Entry
import com.tughi.aggregator.ion.EntryTag
import com.tughi.aggregator.ion.EntryTagRule
import com.tughi.aggregator.ion.Feed
import com.tughi.aggregator.ion.MyFeedTag
import com.tughi.aggregator.ion.Tag
import com.tughi.aggregator.ion.readAggregatorData
import com.tughi.aggregator.ion.readEntry
import com.tughi.aggregator.ion.readEntryTag
import com.tughi.aggregator.ion.readEntryTagRule
import com.tughi.aggregator.ion.readFeed
import com.tughi.aggregator.ion.readMyFeedTag
import com.tughi.aggregator.ion.readTag
import com.tughi.aggregator.ion.writeAggregatorData
import com.tughi.aggregator.ion.writeEntry
import com.tughi.aggregator.ion.writeEntryTag
import com.tughi.aggregator.ion.writeEntryTagRule
import com.tughi.aggregator.ion.writeFeed
import com.tughi.aggregator.ion.writeMyFeedTag
import com.tughi.aggregator.ion.writeTag
import com.tughi.aggregator.ion.writeTypedList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


class BackupService : Service() {
    companion object {
        const val ACTION_CREATE_BACKUP = BuildConfig.APPLICATION_ID + ".intent.action.CREATE_BACKUP"
        const val ACTION_RESTORE_BACKUP = BuildConfig.APPLICATION_ID + ".intent.action.RESTORE_BACKUP"
    }

    private val binder = LocalBinder()

    private var currentJob: Job? = null

    val status = MutableLiveData(Status(false))

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val job = contentScope.launch {
            intent?.data?.let { uri ->
                when (intent.action) {
                    ACTION_CREATE_BACKUP -> {
                        contentResolver.openOutputStream(uri)?.use {
                            GZIPOutputStream(it).use { output ->
                                backup(output)
                            }
                        }
                    }
                    ACTION_RESTORE_BACKUP -> {
                        contentResolver.openInputStream(uri)?.use {
                            GZIPInputStream(it).use { input ->
                                restore(input)
                            }
                        }
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

        Database.transaction {
            val data = AggregatorData(
                version = 1,
                feedsCount = Feeds.queryCount(Feeds.AllCriteria, Feed.QueryHelper),
                entriesCount = Entries.queryCount(AllEntriesQueryCriteria(), Entry.QueryHelper),
                tagsCount = Tags.queryCount(Tags.QueryAllTagsCriteria, Tag.QueryHelper),
                entryTagRulesCount = EntryTagRules.queryCount(EntryTagRules.QueryAllCriteria, EntryTagRule.QueryHelper),
                entryTagsCount = EntryTags.queryCount(EntryTags.QueryAllCriteria, EntryTag.QueryHelper),
                myFeedTagsCount = MyFeedTags.queryCount(MyFeedTags.QueryAllCriteria, MyFeedTag.QueryHelper),
            )
            ionWriter.writeAggregatorData(data)

            val totalRows = (data.feedsCount + data.entriesCount + data.tagsCount + data.entryTagRulesCount + data.entryTagsCount + data.myFeedTagsCount).toFloat()
            var processedRows = 0
            fun updateStatus() {
                if (currentJob!!.isCancelled) {
                    throw CancellationException("Cancelled")
                }

                processedRows += 1
                status.postValue(Status(true, processedRows / totalRows))
            }

            ionWriter.writeTypedList(Feeds::class.simpleName!!, data.feedsCount) {
                Feeds.forEach(Feeds.AllCriteria, Feed.QueryHelper) { feed ->
                    ionWriter.writeFeed(feed)
                    updateStatus()
                }
            }

            ionWriter.writeTypedList(Entries::class.simpleName!!, data.entriesCount) {
                Entries.forEach(AllEntriesQueryCriteria(), Entry.QueryHelper) { entry ->
                    ionWriter.writeEntry(entry)
                    updateStatus()
                }
            }

            ionWriter.writeTypedList(Tags::class.simpleName!!, data.tagsCount) {
                Tags.forEach(Tags.QueryAllTagsCriteria, Tag.QueryHelper) { tag ->
                    ionWriter.writeTag(tag)
                    updateStatus()
                }
            }

            ionWriter.writeTypedList(EntryTagRules::class.simpleName!!, data.entryTagRulesCount) {
                EntryTagRules.forEach(EntryTagRules.QueryAllCriteria, EntryTagRule.QueryHelper) { entryTagRule ->
                    ionWriter.writeEntryTagRule(entryTagRule)
                    updateStatus()
                }
            }

            ionWriter.writeTypedList(EntryTags::class.simpleName!!, data.entryTagsCount) {
                EntryTags.forEach(EntryTags.QueryAllCriteria, EntryTag.QueryHelper) { entryTag ->
                    ionWriter.writeEntryTag(entryTag)
                    updateStatus()
                }
            }

            ionWriter.writeTypedList(MyFeedTags::class.simpleName!!, data.myFeedTagsCount) {
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

    private fun restore(input: InputStream) {
        status.postValue(Status(true))

        val ionReader = IonReaderBuilder.standard().build(input)

        Database.transaction {
            val data = ionReader.readAggregatorData()

            val totalRows = (data.feedsCount + data.entriesCount + data.tagsCount + data.entryTagRulesCount + data.entryTagsCount + data.myFeedTagsCount).toFloat()
            var processedRows = 0
            fun updateStatus() {
                if (currentJob!!.isCancelled) {
                    throw CancellationException("Cancelled")
                }

                processedRows += 1
                status.postValue(Status(true, processedRows / totalRows))
            }

            MyFeedTags.delete(MyFeedTags.DeleteAllCriteria)
            EntryTags.delete(EntryTags.DeleteAllCriteria)
            EntryTagRules.delete(EntryTagRules.DeleteAllCriteria)
            Tags.delete(Tags.DeleteAllCriteria)
            Entries.delete(Entries.DeleteAllCriteria)
            Feeds.delete(Feeds.DeleteAllCriteria)

            while (ionReader.next() == IonType.LIST) {
                val typeAnnotations = ionReader.typeAnnotations
                ionReader.stepIn()
                when {
                    typeAnnotations.contains(Entries::class.simpleName!!) -> {
                        while (ionReader.next() == IonType.STRUCT) {
                            val entryData = ionReader.readEntry()

                            Entries.insert(*entryData)

                            updateStatus()
                        }
                    }
                    typeAnnotations.contains(EntryTags::class.simpleName!!) -> {
                        while (ionReader.next() == IonType.STRUCT) {
                            val entryTagData = ionReader.readEntryTag()

                            EntryTags.insert(*entryTagData)

                            updateStatus()
                        }
                    }
                    typeAnnotations.contains(EntryTagRules::class.simpleName!!) -> {
                        while (ionReader.next() == IonType.STRUCT) {
                            val entryTagRuleData = ionReader.readEntryTagRule()

                            EntryTagRules.insert(*entryTagRuleData)

                            updateStatus()
                        }
                    }
                    typeAnnotations.contains(Feeds::class.simpleName!!) -> {
                        while (ionReader.next() == IonType.STRUCT) {
                            val feedData = ionReader.readFeed()

                            Feeds.insert(*feedData)

                            updateStatus()
                        }
                    }
                    typeAnnotations.contains(MyFeedTags::class.simpleName!!) -> {
                        while (ionReader.next() == IonType.STRUCT) {
                            val myFeedTagData = ionReader.readMyFeedTag()

                            MyFeedTags.insert(*myFeedTagData)

                            updateStatus()
                        }
                    }
                    typeAnnotations.contains(Tags::class.simpleName!!) -> {
                        while (ionReader.next() == IonType.STRUCT) {
                            val tagData = ionReader.readTag()

                            Tags.insert(*tagData)

                            updateStatus()
                        }
                    }
                }
                ionReader.stepOut()
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@BackupService
    }

    data class Status(val busy: Boolean, val progress: Float = 0f)
}
