package com.tughi.aggregator.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ion.util.AbstractValueVisitor
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
import com.tughi.aggregator.ion.entryData
import com.tughi.aggregator.ion.entryTagData
import com.tughi.aggregator.ion.entryTagRuleData
import com.tughi.aggregator.ion.expectAggregatorData
import com.tughi.aggregator.ion.feedData
import com.tughi.aggregator.ion.myFeedTagData
import com.tughi.aggregator.ion.tagData
import com.tughi.aggregator.preferences.UpdateSettings
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

        val ionSystem = IonSystemBuilder.standard().build()
        val ionWriter = IonTextWriterBuilder.pretty().build(output)

        Database.transaction {
            val aggregatorData = AggregatorData(
                version = 1,
                application = AggregatorData.Application(
                    packageName = BuildConfig.APPLICATION_ID,
                    versionCode = BuildConfig.VERSION_CODE,
                    versionName = BuildConfig.VERSION_NAME,
                ),
                updateSettings = AggregatorData.UpdateSettings(
                    backgroundUpdates = UpdateSettings.backgroundUpdates,
                    defaultCleanupMode = UpdateSettings.defaultCleanupMode.serialize(),
                    defaultUpdateMode = UpdateSettings.defaultUpdateMode.serialize(),
                ),
                counters = AggregatorData.Counters(
                    feeds = Feeds.queryCount(Feeds.AllCriteria, Feed.QueryHelper),
                    entries = Entries.queryCount(AllEntriesQueryCriteria(), Entry.QueryHelper),
                    tags = Tags.queryCount(Tags.QueryAllTagsCriteria, Tag.QueryHelper),
                    entryTagRules = EntryTagRules.queryCount(EntryTagRules.QueryAllCriteria, EntryTagRule.QueryHelper),
                    entryTags = EntryTags.queryCount(EntryTags.QueryAllCriteria, EntryTag.QueryHelper),
                    myFeedTags = MyFeedTags.queryCount(MyFeedTags.QueryAllCriteria, MyFeedTag.QueryHelper),
                ),
            )
            aggregatorData.writeTo(ionWriter, ionSystem)

            val totalRows = aggregatorData.counters.total.toFloat()
            var processedRows = 0
            fun updateStatus() {
                if (currentJob!!.isCancelled) {
                    throw CancellationException("Cancelled")
                }

                processedRows += 1
                status.postValue(Status(true, processedRows / totalRows))
            }

            Feeds.forEach(Feeds.AllCriteria, Feed.QueryHelper) { feed ->
                feed.writeTo(ionWriter, ionSystem)
                updateStatus()
            }

            Entries.forEach(AllEntriesQueryCriteria(), Entry.QueryHelper) { entry ->
                entry.writeTo(ionWriter, ionSystem)
                updateStatus()
            }

            Tags.forEach(Tags.QueryAllTagsCriteria, Tag.QueryHelper) { tag ->
                tag.writeTo(ionWriter, ionSystem)
                updateStatus()
            }

            EntryTagRules.forEach(EntryTagRules.QueryAllCriteria, EntryTagRule.QueryHelper) { entryTagRule ->
                entryTagRule.writeTo(ionWriter, ionSystem)
                updateStatus()
            }

            EntryTags.forEach(EntryTags.QueryAllCriteria, EntryTag.QueryHelper) { entryTag ->
                entryTag.writeTo(ionWriter, ionSystem)
                updateStatus()
            }

            MyFeedTags.forEach(MyFeedTags.QueryAllCriteria, MyFeedTag.QueryHelper) { myFeedTag ->
                myFeedTag.writeTo(ionWriter, ionSystem)
                updateStatus()
            }
        }

        ionWriter.finish()
    }

    fun cancel() {
        currentJob?.cancel()
    }

    private fun restore(input: InputStream) {
        status.postValue(Status(true))

        val ionSystem = IonSystemBuilder.standard().build()
        val ionReader = IonReaderBuilder.standard().build(input)

        val ionValueIterator = ionSystem.iterate(ionReader)

        val aggregatorData = ionValueIterator.expectAggregatorData()

        val totalRows = aggregatorData.counters.total.toFloat()
        var processedRows = 0

        Database.transaction {
            MyFeedTags.delete(MyFeedTags.DeleteAllCriteria)
            EntryTags.delete(EntryTags.DeleteAllCriteria)
            EntryTagRules.delete(EntryTagRules.DeleteAllCriteria)
            Tags.delete(Tags.DeleteAllCriteria)
            Entries.delete(Entries.DeleteAllCriteria)
            Feeds.delete(Feeds.DeleteAllCriteria)

            val visitor = object : AbstractValueVisitor() {
                override fun defaultVisit(ionValue: IonValue) {
                    throw IllegalStateException("Unsupported value type: $ionValue")
                }

                override fun visit(ionStruct: IonStruct) {
                    when (ionStruct.typeAnnotations?.first()) {
                        Entry::class.simpleName -> Entries.insert(*entryData(ionStruct))
                        EntryTag::class.simpleName -> EntryTags.insert(*entryTagData(ionStruct))
                        EntryTagRule::class.simpleName -> EntryTagRules.insert(*entryTagRuleData(ionStruct))
                        Feed::class.simpleName -> Feeds.insert(*feedData(ionStruct))
                        MyFeedTag::class.simpleName -> MyFeedTags.insert(*myFeedTagData(ionStruct))
                        Tag::class.simpleName -> Tags.insert(*tagData(ionStruct))
                        else -> super.visit(ionStruct)
                    }

                    processedRows += 1
                    status.postValue(Status(true, processedRows / totalRows))
                }
            }

            while (ionValueIterator.hasNext()) {
                if (currentJob!!.isCancelled) {
                    throw CancellationException("Cancelled")
                }

                ionValueIterator.next().accept(visitor)
            }
        }

        // TODO: apply update settings
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@BackupService
    }

    data class Status(val busy: Boolean, val progress: Float = 0f)
}
