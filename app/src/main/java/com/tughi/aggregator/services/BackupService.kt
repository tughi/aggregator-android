package com.tughi.aggregator.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionelement.api.IonElementLoaderOptions
import com.amazon.ionelement.api.createIonElementLoader
import com.amazon.ionelement.api.location
import com.amazon.ionelement.api.locationToString
import com.tughi.aggregator.BuildConfig
import com.tughi.aggregator.R
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.AllEntriesQueryCriteria
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.EntryTags
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.MyFeedTags
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.ion.AggregatorData
import com.tughi.aggregator.ion.Entry
import com.tughi.aggregator.ion.EntryTag
import com.tughi.aggregator.ion.EntryTagRule
import com.tughi.aggregator.ion.Feed
import com.tughi.aggregator.ion.MyFeedTag
import com.tughi.aggregator.ion.Tag
import com.tughi.aggregator.preferences.UpdateSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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

    val message = MutableLiveData<String>()
    val status = MutableLiveData(Status(false))

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val job = contentScope.launch {
            intent?.data?.let { uri ->
                when (intent.action) {
                    ACTION_CREATE_BACKUP -> {
                        message.postValue(getString(R.string.backup__backup__message))
                        contentResolver.openOutputStream(uri)?.use {
                            GZIPOutputStream(it).use { output ->
                                backup(output)
                            }
                        }
                    }
                    ACTION_RESTORE_BACKUP -> {
                        message.postValue(getString(R.string.backup__restore__message))
                        contentResolver.openInputStream(uri)?.use {
                            try {
                                GZIPInputStream(it).use { input ->
                                    restore(input)
                                }
                            } catch (error: Throwable) {
                                Log.w(this@BackupService::class.qualifiedName, "Unsupported file format", error)

                                launch(Dispatchers.Main) {
                                    Toast.makeText(this@BackupService, R.string.backup__error__unsupported_file_format, Toast.LENGTH_LONG).show()
                                }
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
            aggregatorData.writeTo(ionWriter)

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
                feed.writeTo(ionWriter)
                updateStatus()
            }

            Entries.forEach(AllEntriesQueryCriteria(), Entry.QueryHelper) { entry ->
                entry.writeTo(ionWriter)
                updateStatus()
            }

            Tags.forEach(Tags.QueryAllTagsCriteria, Tag.QueryHelper) { tag ->
                tag.writeTo(ionWriter)
                updateStatus()
            }

            EntryTagRules.forEach(EntryTagRules.QueryAllCriteria, EntryTagRule.QueryHelper) { entryTagRule ->
                entryTagRule.writeTo(ionWriter)
                updateStatus()
            }

            EntryTags.forEach(EntryTags.QueryAllCriteria, EntryTag.QueryHelper) { entryTag ->
                entryTag.writeTo(ionWriter)
                updateStatus()
            }

            MyFeedTags.forEach(MyFeedTags.QueryAllCriteria, MyFeedTag.QueryHelper) { myFeedTag ->
                myFeedTag.writeTo(ionWriter)
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

        val ionReader = IonReaderBuilder.standard().build(input)

        val elementLoader = createIonElementLoader(options = IonElementLoaderOptions(includeLocationMeta = true))

        ionReader.next()
        val aggregatorData = AggregatorData(elementLoader.loadCurrentElement(ionReader).asStruct())

        val totalRows = aggregatorData.counters.total.toFloat()
        var processedRows = 0

        Database.transaction {
            MyFeedTags.delete(MyFeedTags.DeleteAllCriteria)
            EntryTags.delete(EntryTags.DeleteAllCriteria)
            EntryTagRules.delete(EntryTagRules.DeleteAllCriteria)
            Tags.delete(Tags.DeleteAllCriteria)
            Entries.delete(Entries.DeleteAllCriteria)
            Feeds.delete(Feeds.DeleteAllCriteria)

            while (ionReader.next() != null) {
                if (currentJob!!.isCancelled) {
                    throw CancellationException("Cancelled")
                }

                elementLoader.loadCurrentElement(ionReader).also { element ->
                    when (element.annotations.let { if (it.isNotEmpty()) it[0] else null }) {
                        Entry::class.simpleName -> Entries.insert(Entry(element.asStruct()))
                        EntryTag::class.simpleName -> EntryTags.insert(EntryTag(element.asStruct()))
                        EntryTagRule::class.simpleName -> EntryTagRules.insert(EntryTagRule(element.asStruct()))
                        Feed::class.simpleName -> Feeds.insert(Feed(element.asStruct()))
                        MyFeedTag::class.simpleName -> MyFeedTags.insert(MyFeedTag(element.asStruct()))
                        Tag::class.simpleName -> Tags.insert(Tag(element.asStruct()))
                        else -> throw IllegalStateException("${locationToString(element.metas.location)}: Unsupported element: $element")
                    }

                    processedRows += 1
                    status.postValue(Status(true, processedRows / totalRows))
                }
            }
        }

        UpdateSettings.backgroundUpdates = aggregatorData.updateSettings.backgroundUpdates
        UpdateSettings.defaultCleanupMode = CleanupMode.deserialize(aggregatorData.updateSettings.defaultCleanupMode)
        UpdateSettings.defaultUpdateMode = UpdateMode.deserialize(aggregatorData.updateSettings.defaultUpdateMode)
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@BackupService
    }

    data class Status(val busy: Boolean, val progress: Float = 0f)
}
