package com.tughi.aggregator.activities.opml

import android.app.Activity
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.feeds.OpmlFeed
import com.tughi.aggregator.feeds.OpmlParser
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FaviconUpdateScheduler
import com.tughi.aggregator.utilities.has
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class OpmlImportActivity : AppActivity() {
    private lateinit var viewModel: OpmlFeedsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.opml_import_activity)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_back)
        }

        viewModel = ViewModelProvider(this).get()

        val subscribeButton = findViewById<Button>(R.id.subscribe)
        subscribeButton.setOnClickListener {
            viewModel.feeds.value?.let { feeds ->
                importFeeds(feeds)
            }
        }

        viewModel.feeds.observe(this) { feeds ->
            subscribeButton.isEnabled = feeds.has { feed -> feed.selected }
        }

        if (savedInstanceState == null) {
            intent.data?.let {
                parseOpml(it)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun parseOpml(uri: Uri) {
        val contentResolver = contentResolver

        contentScope.launch {
            val subscriptions = Feeds.query(Feeds.AllCriteria, object : Feeds.QueryHelper<String>(Feeds.URL) {
                override fun createRow(cursor: Cursor): String = cursor.getString(0)
            }).toSet()

            contentResolver?.openInputStream(uri)?.use {
                val feeds = mutableListOf<OpmlFeed>()

                try {
                    OpmlParser.parse(it, object : OpmlParser.Listener {
                        override fun onFeedParsed(feed: OpmlFeed) {
                            if (subscriptions.contains(feed.url)) {
                                feeds.add(feed.copy(aggregated = true, selected = false))
                            } else {
                                feeds.add(feed)
                            }
                        }
                    })
                } catch (exception: IOException) {
                    Log.e(javaClass.name, "Failed to load OPML file", exception)
                }

                viewModel.feeds.postValue(feeds)
            }
        }
    }

    private fun importFeeds(feeds: List<OpmlFeed>) {
        contentScope.launch {
            feeds.forEach {
                if (it.selected) {
                    val feedId = Feeds.insert(
                        Feeds.URL to it.url,
                        Feeds.LINK to it.link,
                        Feeds.TITLE to it.title,
                        Feeds.CUSTOM_TITLE to it.customTitle,
                        Feeds.UPDATE_MODE to it.updateMode.serialize()
                    )

                    if (feedId > 0) {
                        Feeds.update(
                            Feeds.UpdateRowCriteria(feedId),
                            Feeds.NEXT_UPDATE_TIME to AutoUpdateScheduler.calculateNextUpdateTime(feedId, it.updateMode, 0)
                        )
                    }
                }
            }

            AutoUpdateScheduler.schedule()

            launch(Dispatchers.Main) {
                FaviconUpdateScheduler.schedule()
            }
        }

        setResult(Activity.RESULT_OK)
        finish()
    }
}
