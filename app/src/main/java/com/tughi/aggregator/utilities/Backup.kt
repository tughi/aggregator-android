package com.tughi.aggregator.utilities

import com.tughi.aggregator.App
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.feeds.OpmlFeed
import com.tughi.aggregator.feeds.OpmlGenerator
import com.tughi.aggregator.feeds.OpmlParser
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FaviconUpdateScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

private const val BACKUP_FILENAME = "feeds.opml"

fun backupFeeds() {
    App.instance.getExternalFilesDir(null)?.also { externalFilesDir ->
        val backupFile = File(externalFilesDir, BACKUP_FILENAME)
        backupFile.parentFile?.mkdirs()

        backupFile.outputStream().use { outputStream ->
            OpmlGenerator.generate(Feeds.query(Feeds.AllCriteria, OpmlFeed.QueryHelper), outputStream)
        }
    }
}

fun restoreFeeds() {
    if (Feeds.queryAllCount() == 0) {
        App.instance.getExternalFilesDir(null)?.also { externalFilesDir ->
            val backupFile = File(externalFilesDir, BACKUP_FILENAME)
            if (backupFile.exists()) {
                backupFile.inputStream().use { inputStream ->
                    Database.transaction {
                        OpmlParser.parse(inputStream, object : OpmlParser.Listener {
                            override fun onFeedParsed(feed: OpmlFeed) {
                                val feedId = Feeds.insert(
                                        Feeds.URL to feed.url,
                                        Feeds.TITLE to feed.title,
                                        Feeds.CUSTOM_TITLE to feed.customTitle,
                                        Feeds.LINK to feed.link,
                                        Feeds.UPDATE_MODE to feed.updateMode
                                )

                                if (feedId > 0) {
                                    Feeds.update(
                                            Feeds.UpdateRowCriteria(feedId),
                                            Feeds.NEXT_UPDATE_TIME to AutoUpdateScheduler.calculateNextUpdateTime(feedId, feed.updateMode, 0)
                                    )
                                }
                            }
                        })
                    }

                    AutoUpdateScheduler.schedule()

                    FaviconUpdateScheduler.schedule()
                }
            }
        }
    }
}
