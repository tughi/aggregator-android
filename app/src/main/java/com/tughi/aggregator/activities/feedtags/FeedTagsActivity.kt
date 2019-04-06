package com.tughi.aggregator.activities.feedtags

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.tagsettings.TagSettingsActivity
import com.tughi.aggregator.data.FeedTags
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.utilities.has
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FeedTagsActivity : AppActivity() {

    companion object {
        private const val EXTRA_FEED_ID = "feed_id"

        fun start(context: Context, feedId: Long) {
            context.startActivity(
                    Intent(context, FeedTagsActivity::class.java)
                            .putExtra(EXTRA_FEED_ID, feedId)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val feedId = intent.getLongExtra(EXTRA_FEED_ID, 0)

        setContentView(R.layout.simple_list)
        val recyclerView = findViewById<RecyclerView>(R.id.list)
        val progressBar = findViewById<ProgressBar>(R.id.progress)

        val adapter = TagsAdapter(object : TagsAdapter.Listener {
            override fun onTagClick(tag: Tag) {
                GlobalScope.launch {
                    if (tag.feedTag) {
                        FeedTags.delete(FeedTags.DeleteFeedTagCriteria(feedId, tag.id))
                    } else {
                        FeedTags.insert(
                                FeedTags.FEED_ID to feedId,
                                FeedTags.TAG_ID to tag.id,
                                FeedTags.TAG_TIME to System.currentTimeMillis()
                        )
                    }
                }
            }
        })

        recyclerView.adapter = adapter

        val viewModelFactory = FeedTagsViewModel.Factory(feedId)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(FeedTagsViewModel::class.java)
        viewModel.tags.observe(this, Observer { tags ->
            if (tags == null) {
                adapter.tags = emptyList()
                progressBar.visibility = View.VISIBLE
            } else {
                adapter.tags = tags
                progressBar.visibility = View.GONE
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.entry_tags_activity, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.add -> {
            TagSettingsActivity.start(this, null)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    data class Tag(val id: Long, val name: String, val feedTag: Boolean = false) {
        object QueryHelper : Tags.QueryHelper<Tag>(
                Tags.ID,
                Tags.NAME
        ) {
            override fun createRow(cursor: Cursor) = Tag(
                    id = cursor.getLong(0),
                    name = cursor.getString(1)
            )
        }
    }

    class FeedTag(val tagId: Long) {
        object QueryHelper : FeedTags.QueryHelper<FeedTag>(
                FeedTags.TAG_ID
        ) {
            override fun createRow(cursor: Cursor) = FeedTag(
                    tagId = cursor.getLong(0)
            )
        }
    }

    class FeedTagsViewModel(feedId: Long) : ViewModel() {
        val tags = MediatorLiveData<List<Tag>>()

        init {
            val tags = Tags.liveQuery(Tags.QueryVisibleTagsCriteria, Tag.QueryHelper)
            val feedTags = FeedTags.liveQuery(FeedTags.QueryFeedTagsCriteria(feedId), FeedTag.QueryHelper)

            this.tags.addSource(tags) { mergeTags(it, feedTags.value) }

            this.tags.addSource(feedTags) { mergeTags(tags.value, it) }
        }

        private fun mergeTags(tags: List<Tag>?, feedTags: List<FeedTag>?) {
            this.tags.value = when {
                tags == null || feedTags == null -> emptyList()
                feedTags.isEmpty() -> tags
                else -> tags.map { tag ->
                    tag.copy(
                            feedTag = feedTags.has { it.tagId == tag.id }
                    )
                }
            }
        }

        class Factory(private val feedId: Long) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FeedTagsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return FeedTagsViewModel(feedId) as T
                }
                throw UnsupportedOperationException()
            }
        }
    }

    class TagViewHolder(itemView: View, listener: TagsAdapter.Listener) : RecyclerView.ViewHolder(itemView) {
        private var tag: Tag? = null

        private val faviconView: ImageView = itemView.findViewById(R.id.favicon)
        private val textView: CheckedTextView = itemView.findViewById(R.id.text)

        init {
            itemView.setOnClickListener {
                tag?.let { tag -> listener.onTagClick(tag) }
            }
        }

        fun onBind(tag: Tag) {
            this.tag = tag

            faviconView.setImageResource(when (tag.id) {
                Tags.STARRED -> R.drawable.favicon_star
                else -> R.drawable.favicon_tag
            })

            textView.text = tag.name
            textView.isChecked = tag.feedTag
        }
    }

    class TagsAdapter(private val listener: Listener) : RecyclerView.Adapter<TagViewHolder>() {
        var tags: List<Tag> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = tags.size

        override fun getItemId(position: Int): Long = tags[position].id

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TagViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.entry_tags_item, parent, false),
                listener
        )

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) = holder.onBind(tags[position])

        interface Listener {
            fun onTagClick(tag: Tag)
        }
    }

}
