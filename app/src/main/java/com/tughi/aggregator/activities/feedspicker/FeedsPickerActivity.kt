package com.tughi.aggregator.activities.feedspicker

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.collection.LongSparseArray
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.subscribe.SubscribeActivity
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.utilities.Favicons

class FeedsPickerActivity : AppActivity() {

    companion object {
        const val EXTRA_SELECTED_FEEDS = "selected_feeds"
        private const val EXTRA_TITLE = "title"

        fun startForResult(activity: Activity, resultCode: Int, selectedFeeds: LongArray, title: String?) {
            activity.startActivityForResult(
                    Intent(activity, FeedsPickerActivity::class.java)
                            .putExtra(EXTRA_SELECTED_FEEDS, selectedFeeds)
                            .putExtra(EXTRA_TITLE, title),
                    resultCode
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra(EXTRA_TITLE)?.let {
            title = it
        }

        val selectedFeedIds = intent.getLongArrayExtra(EXTRA_SELECTED_FEEDS) ?: LongArray(0)
        val viewModelFactory = FeedViewModel.Factory(selectedFeedIds)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(FeedViewModel::class.java)

        setContentView(R.layout.feeds_picker_activity)
        val recyclerView = findViewById<RecyclerView>(R.id.list)
        val progressBar = findViewById<ProgressBar>(R.id.progress)

        val adapter = FeedsAdapter(object : FeedsAdapter.Listener {
            override fun onFeedClick(feed: Feed) {
                viewModel.toggleFeed(feed.id)
            }
        })

        recyclerView.adapter = adapter

        viewModel.feeds.observe(this, Observer { feeds ->
            if (feeds == null) {
                adapter.feeds = emptyList()
                progressBar.visibility = View.VISIBLE
            } else {
                adapter.feeds = feeds
                progressBar.visibility = View.GONE
            }
        })

        findViewById<Button>(R.id.select).setOnClickListener {
            val selectedFeeds = viewModel.feeds.value?.filter { it.selected } ?: emptyList()
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_SELECTED_FEEDS, LongArray(selectedFeeds.size) { selectedFeeds[it].id }))

            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.feeds_picker_activity, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.add -> {
            SubscribeActivity.start(this)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    data class Feed(val id: Long, val title: String, val faviconUrl: String?, val selected: Boolean = false) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
                Feeds.ID,
                Feeds.CUSTOM_TITLE,
                Feeds.TITLE,
                Feeds.FAVICON_URL
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                    id = cursor.getLong(0),
                    title = cursor.getString(1) ?: cursor.getString(2),
                    faviconUrl = cursor.getString(3)
            )
        }
    }

    class FeedViewModel(initialSelectedFeedIds: LongArray) : ViewModel() {
        private val selectedFeedIds = MutableLiveData<LongSparseArray<Boolean>>().apply {
            val selectedFeeds = LongSparseArray<Boolean>()
            for (selectedFeed in initialSelectedFeedIds) {
                selectedFeeds.put(selectedFeed, true)
            }
            value = selectedFeeds
        }

        val feeds = MediatorLiveData<List<Feed>>()

        init {
            val feeds = Feeds.liveQuery(Feeds.AllCriteria, Feed.QueryHelper)

            this.feeds.addSource(this.selectedFeedIds) { merge(feeds.value, it) }
            this.feeds.addSource(feeds) { merge(it, this.selectedFeedIds.value) }
        }

        private fun merge(feeds: List<Feed>?, selectedFeedIds: LongSparseArray<Boolean>?) {
            this.feeds.value = when {
                feeds == null || selectedFeedIds == null -> emptyList()
                selectedFeedIds.isEmpty -> feeds
                else -> feeds.map { feed ->
                    feed.copy(
                            selected = selectedFeedIds.get(feed.id, false)
                    )
                }
            }
        }

        fun toggleFeed(feedId: Long) {
            selectedFeedIds.apply {
                val selectedFeeds = value ?: LongSparseArray()
                selectedFeeds.put(feedId, !selectedFeeds.get(feedId, false))
                value = selectedFeeds
            }
        }

        class Factory(private val selectedFeedIds: LongArray) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return FeedViewModel(selectedFeedIds) as T
                }
                throw UnsupportedOperationException()
            }
        }
    }

    class FeedViewHolder(itemView: View, listener: FeedsAdapter.Listener) : RecyclerView.ViewHolder(itemView) {
        private var feed: Feed? = null

        private val faviconView: ImageView = itemView.findViewById(R.id.favicon)
        private val textView: CheckedTextView = itemView.findViewById(R.id.text)

        init {
            itemView.setOnClickListener {
                feed?.let { feed -> listener.onFeedClick(feed) }
            }
        }

        fun onBind(feed: Feed) {
            this.feed = feed

            Favicons.load(feed.id, feed.faviconUrl, faviconView)

            textView.text = feed.title
            textView.isChecked = feed.selected
        }
    }

    class FeedsAdapter(private val listener: Listener) : RecyclerView.Adapter<FeedViewHolder>() {
        var feeds: List<Feed> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = feeds.size

        override fun getItemId(position: Int): Long = feeds[position].id

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FeedViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.feeds_picker_item, parent, false),
                listener
        )

        override fun onBindViewHolder(holder: FeedViewHolder, position: Int) = holder.onBind(feeds[position])

        interface Listener {
            fun onFeedClick(feed: Feed)
        }
    }

}
