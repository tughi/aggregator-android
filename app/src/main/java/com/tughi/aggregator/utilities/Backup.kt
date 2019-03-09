package com.tughi.aggregator.utilities

import com.tughi.aggregator.App
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.feeds.OpmlGenerator
import com.tughi.aggregator.feeds.OpmlParser
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FaviconUpdaterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

private const val BACKUP_FILENAME = "feeds.opml"

fun backupFeeds() {
    App.instance.getExternalFilesDir(null)?.also { externalFilesDir ->
        val backupFile = File(externalFilesDir, BACKUP_FILENAME)
        backupFile.parentFile.mkdirs()

        backupFile.outputStream().use { outputStream ->
            val repository = OpmlGenerator.repository
            OpmlGenerator.generate(repository.query(repository.AllCriteria()), outputStream)
        }
    }
}

fun restoreFeeds() {
    if (Feeds.count() == 0) {
        App.instance.getExternalFilesDir(null)?.also { externalFilesDir ->
            val backupFile = File(externalFilesDir, BACKUP_FILENAME)
            if (backupFile.exists()) {
                backupFile.inputStream().use { inputStream ->
                    OpmlParser.parse(inputStream, object : OpmlParser.Listener {
                        override fun onFeedParsed(url: String, title: String, link: String?, customTitle: String?, category: String?, updateMode: UpdateMode) {
                            val repository = OpmlGenerator.repository

                            val feedId = repository.insert(
                                    Feeds.URL to url,
                                    Feeds.TITLE to title,
                                    Feeds.LINK to link,
                                    Feeds.CUSTOM_TITLE to customTitle,
                                    Feeds.UPDATE_MODE to updateMode
                            )

                            if (feedId > 0) {
                                repository.update(
                                        feedId,
                                        Feeds.NEXT_UPDATE_TIME to AutoUpdateScheduler.calculateNextUpdateTime(feedId, updateMode, 0)
                                )

                                GlobalScope.launch(Dispatchers.Main) {
                                    FaviconUpdaterService.start(feedId)
                                }
                            }
                        }
                    })

                    AutoUpdateScheduler.schedule()
                }
            }
        }
    }
}
