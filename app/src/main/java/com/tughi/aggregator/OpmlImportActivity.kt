package com.tughi.aggregator

import android.app.Activity
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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.feeds.OpmlFeed
import com.tughi.aggregator.feeds.OpmlParser
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.services.FaviconUpdateScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

internal class OpmlImportViewModel : ViewModel() {

    val feeds = MutableLiveData<List<OpmlFeed>>()

    fun parseOpml(uri: Uri) {
        GlobalScope.launch(Dispatchers.IO) {
            App.instance.contentResolver?.openInputStream(uri)?.use {
                val feeds = mutableListOf<OpmlFeed>()

                try {
                    OpmlParser.parse(it, object : OpmlParser.Listener {
                        override fun onFeedParsed(feed: OpmlFeed) {
                            feeds.add(feed)
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
            newFeeds.add(it.copy(excluded = !it.excluded))
        }

        feeds.value = newFeeds
    }

    fun toggleFeed(feed: OpmlFeed) {
        val newFeeds = mutableListOf<OpmlFeed>()

        feeds.value?.forEach {
            if (it === feed) {
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

        viewModel = ViewModelProvider(this).get(OpmlImportViewModel::class.java)

        val feedsAdapter = OpmlFeedsAdapter(viewModel)

        findViewById<RecyclerView>(R.id.feeds).adapter = feedsAdapter

        val subscribeButton = findViewById<Button>(R.id.subscribe)

        subscribeButton.setOnClickListener {
            viewModel.feeds.value?.let { feeds ->
                GlobalScope.launch(Dispatchers.IO) {
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

                                GlobalScope.launch(Dispatchers.Main) {
                                    FaviconUpdateScheduler.schedule(feedId)
                                }
                            }
                        }
                    }

                    AutoUpdateScheduler.schedule()
                }

                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        viewModel.feeds.observe(this, Observer { feeds ->
            feedsAdapter.submitList(feeds)

            val feedCount = feeds.size

            var excludedFeedCount = 0
            feeds.forEach { feed ->
                if (feed.excluded) {
                    excludedFeedCount++
                }
            }

            subscribeButton.isEnabled = feedCount != 0 && feedCount != excludedFeedCount
        })

        if (savedInstanceState == null) {
            intent.data?.let {
                viewModel.parseOpml(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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

        if (feed.excluded) {
            checkbox.setImageResource(R.drawable.check_box_unchecked)
        } else {
            checkbox.setImageResource(R.drawable.check_box_checked)
        }
        title.text = feed.customTitle ?: feed.title
        url.text = feed.url
    }

}

internal class OpmlFeedsAdapter(val viewModel: OpmlImportViewModel) : ListAdapter<OpmlFeed, OpmlFeedViewHolder>(FeedDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpmlFeedViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.opml_import_list_item, parent, false)
        val viewHolder = OpmlFeedViewHolder(itemView)

        itemView.setOnClickListener {
            viewModel.toggleFeed(viewHolder.feed)
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: OpmlFeedViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

}

object FeedDiffUtil : DiffUtil.ItemCallback<OpmlFeed>() {

    override fun areItemsTheSame(oldItem: OpmlFeed, newItem: OpmlFeed) = oldItem === newItem

    override fun areContentsTheSame(oldItem: OpmlFeed, newItem: OpmlFeed) = oldItem == newItem

}
