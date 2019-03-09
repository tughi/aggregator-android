package com.tughi.aggregator.utilities

import com.tughi.aggregator.App
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.data.Feed
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
            OpmlGenerator.generate(OpmlGenerator.repository.query(OpmlGenerator.repository.AllCriteria()), outputStream)
        }
    }
}

fun restoreFeeds() {
    val feedDao = AppDatabase.instance.feedDao()
    if (feedDao.queryFeedCount() == 0) {
        App.instance.getExternalFilesDir(null)?.also { externalFilesDir ->
            val backupFile = File(externalFilesDir, BACKUP_FILENAME)
            if (backupFile.exists()) {
                backupFile.inputStream().use { inputStream ->
                    OpmlParser.parse(inputStream, object : OpmlParser.Listener {
                        override fun onFeedParsed(url: String, title: String, link: String?, customTitle: String?, category: String?, updateMode: UpdateMode) {
                            val feedId = feedDao.insertFeed(Feed(
                                    url = url,
                                    title = title,
                                    link = link,
                                    customTitle = customTitle,
                                    updateMode = updateMode
                            ))

                            if (feedId > 0) {
                                feedDao.updateFeed(feedId, AutoUpdateScheduler.calculateNextUpdateTime(feedId, updateMode, 0))

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
