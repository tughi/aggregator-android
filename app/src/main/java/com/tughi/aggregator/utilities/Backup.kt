package com.tughi.aggregator.utilities

import com.tughi.aggregator.App
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.feeds.OpmlFeed
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
                        override fun onFeedParsed(feed: OpmlFeed) {
                            val repository = OpmlGenerator.repository

                            val feedId = repository.insert(
                                    Feeds.URL to feed.url,
                                    Feeds.TITLE to feed.title,
                                    Feeds.CUSTOM_TITLE to feed.customTitle,
                                    Feeds.LINK to feed.link,
                                    Feeds.UPDATE_MODE to feed.updateMode
                            )

                            if (feedId > 0) {
                                repository.update(
                                        feedId,
                                        Feeds.NEXT_UPDATE_TIME to AutoUpdateScheduler.calculateNextUpdateTime(feedId, feed.updateMode, 0)
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
