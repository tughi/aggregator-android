package com.tughi.aggregator.activities.opml

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.App
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.feeds.OpmlFeed
import com.tughi.aggregator.feeds.OpmlParser
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FaviconUpdateScheduler
import com.tughi.aggregator.utilities.backupFeeds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

internal class OpmlImportViewModel : ViewModel() {
    val feeds = MutableLiveData<List<OpmlFeed>>()

    fun parseOpml(uri: Uri) {
        contentScope.launch {
            val subscriptions = Feeds.query(Feeds.AllCriteria, object : Feeds.QueryHelper<String>(Feeds.URL) {
                override fun createRow(cursor: Cursor): String = cursor.getString(0)
            }).toSet()

            App.instance.contentResolver?.openInputStream(uri)?.use {
                val feeds = mutableListOf<OpmlFeed>()

                try {
                    OpmlParser.parse(it, object : OpmlParser.Listener {
                        override fun onFeedParsed(feed: OpmlFeed) {
                            if (subscriptions.contains(feed.url)) {
                                feeds.add(feed.copy(enabled = false, excluded = true))
                            } else {
                                feeds.add(feed)
                            }
                        }
                    })
                } catch (exception: IOException) {
                    Log.e(javaClass.name, "Failed to load OPML file", exception)
                }

                launch(Dispatchers.Main) {
                    this@OpmlImportViewModel.feeds.value = feeds
                }
            }
        }
    }

    fun toggleAllFeeds() {
        val newFeeds = mutableListOf<OpmlFeed>()

        feeds.value?.forEach {
            if (it.enabled) {
                newFeeds.add(it.copy(excluded = !it.excluded))
            } else {
                newFeeds.add(it)
            }
        }

        feeds.value = newFeeds
    }

    fun toggleFeed(feed: OpmlFeed) {
        val newFeeds = mutableListOf<OpmlFeed>()

        feeds.value?.forEach {
            if (it === feed && feed.enabled) {
                newFeeds.add(it.copy(excluded = !it.excluded))
            } else {
                newFeeds.add(it)
            }
        }

        feeds.value = newFeeds
    }
}

class OpmlImportActivity : AppActivity() {
    private lateinit var viewModel: OpmlImportViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.opml_import_activity)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_back)
        }

        viewModel = ViewModelProvider(this)[OpmlImportViewModel::class.java]

        val feedsAdapter = OpmlFeedsAdapter(viewModel)

        findViewById<RecyclerView>(R.id.feeds).adapter = feedsAdapter

        val subscribeButton = findViewById<Button>(R.id.subscribe)

        subscribeButton.setOnClickListener {
            viewModel.feeds.value?.let { feeds ->
                contentScope.launch {
                    feeds.forEach {
                        if (!it.excluded) {
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

                    backupFeeds()
                }

                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        viewModel.feeds.observe(this) { feeds ->
            feedsAdapter.feeds = feeds

            val feedCount = feeds.size

            var excludedFeedCount = 0
            feeds.forEach { feed ->
                if (feed.excluded) {
                    excludedFeedCount++
                }
            }

            subscribeButton.isEnabled = feedCount != 0 && feedCount != excludedFeedCount
        }

        if (savedInstanceState == null) {
            intent.data?.let {
                viewModel.parseOpml(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.opml_import_activity, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.invert_selection -> viewModel.toggleAllFeeds()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }
}

internal class OpmlFeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val checkbox: ImageView = itemView.findViewById(R.id.checkbox)
    val title: TextView = itemView.findViewById(R.id.title)
    val url: TextView = itemView.findViewById(R.id.url)

    lateinit var feed: OpmlFeed

    fun onBind(feed: OpmlFeed) {
        this.feed = feed

        itemView.isEnabled = feed.enabled

        checkbox.isEnabled = feed.enabled
        if (feed.excluded) {
            checkbox.setImageResource(R.drawable.check_box_unchecked)
        } else {
            checkbox.setImageResource(R.drawable.check_box_checked)
        }

        title.isEnabled = feed.enabled
        title.text = feed.customTitle ?: feed.title

        url.isEnabled = feed.enabled
        url.text = feed.url
    }
}

internal class OpmlFeedsAdapter(val viewModel: OpmlImportViewModel) : RecyclerView.Adapter<OpmlFeedViewHolder>() {
    var feeds = emptyList<OpmlFeed>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = feeds.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpmlFeedViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.opml_import_list_item, parent, false)
        val viewHolder = OpmlFeedViewHolder(itemView)

        itemView.setOnClickListener {
            viewModel.toggleFeed(viewHolder.feed)
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: OpmlFeedViewHolder, position: Int) {
        holder.onBind(feeds[position])
    }
}

class ImportOpmlResultContract : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent =
        Intent(context, OpmlImportActivity::class.java).apply {
            this.data = input
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}
