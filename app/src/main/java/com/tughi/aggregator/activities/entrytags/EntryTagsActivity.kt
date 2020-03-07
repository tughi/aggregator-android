package com.tughi.aggregator.activities.entrytags

import android.content.Context
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
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.tagsettings.TagSettingsActivity
import com.tughi.aggregator.data.EntryTags
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.utilities.has
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EntryTagsActivity : AppActivity() {

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"

        fun start(context: Context, entryId: Long) {
            context.startActivity(
                    Intent(context, EntryTagsActivity::class.java)
                            .putExtra(EXTRA_ENTRY_ID, entryId)
            )
        }
    }

    private val entryId by lazy { intent.getLongExtra(EXTRA_ENTRY_ID, 0) }

    private val viewModel: EntryTagsViewModel by lazy {
        val viewModelFactory = EntryTagsViewModel.Factory(entryId)
        ViewModelProvider(this, viewModelFactory).get(EntryTagsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.entry_tags_activity)
        val recyclerView = findViewById<RecyclerView>(R.id.list)
        val progressBar = findViewById<ProgressBar>(R.id.progress)

        val adapter = TagsAdapter(object : TagsAdapter.Listener {
            override fun onTagClick(tag: Tag) {
                tag.newEntryTag = !tag.newEntryTag
                recyclerView.adapter?.notifyDataSetChanged()
            }
        })

        recyclerView.adapter = adapter

        viewModel.tags.observe(this, Observer { tags ->
            if (tags == null) {
                adapter.tags = emptyList()
                progressBar.visibility = View.VISIBLE
            } else {
                adapter.tags = tags
                progressBar.visibility = View.GONE
            }
        })

        findViewById<Button>(R.id.save).setOnClickListener {
            onSave()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.entry_tags_activity, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.add -> {
            TagSettingsActivity.start(this, null)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onSave() {
        val tags = viewModel.tags.value ?: return

        GlobalScope.launch {
            for (tag in tags) {
                if (tag.newEntryTag != tag.oldEntryTag) {
                    if (tag.newEntryTag) {
                        EntryTags.insert(
                                EntryTags.ENTRY_ID to entryId,
                                EntryTags.TAG_ID to tag.id,
                                EntryTags.TAG_TIME to System.currentTimeMillis()
                        )
                    } else {
                        EntryTags.delete(EntryTags.DeleteEntryTagCriteria(entryId, tag.id))
                    }
                }
            }
        }

        finish()
    }

    data class Tag(val id: Long, val name: String, val oldEntryTag: Boolean = false, var newEntryTag: Boolean = false) {
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

    class EntryTag(val tagId: Long) {
        object QueryHelper : EntryTags.QueryHelper<EntryTag>(
                EntryTags.TAG_ID
        ) {
            override fun createRow(cursor: Cursor) = EntryTag(
                    tagId = cursor.getLong(0)
            )
        }
    }

    class EntryTagsViewModel(entryId: Long) : ViewModel() {
        val tags = MediatorLiveData<List<Tag>>()

        init {
            val tags = Tags.liveQuery(Tags.QueryUserTagsCriteria, Tag.QueryHelper)
            val entryTags = EntryTags.liveQuery(EntryTags.QueryEntryTagsCriteria(entryId), EntryTag.QueryHelper)

            this.tags.addSource(tags) { mergeTags(it, entryTags.value) }
            this.tags.addSource(entryTags) { mergeTags(tags.value, it) }
        }

        private fun mergeTags(tags: List<Tag>?, entryTags: List<EntryTag>?) {
            this.tags.value = when {
                tags == null || entryTags == null -> emptyList()
                entryTags.isEmpty() -> tags
                else -> tags.map { tag ->
                    tag.copy(
                            oldEntryTag = entryTags.has { it.tagId == tag.id },
                            newEntryTag = entryTags.has { it.tagId == tag.id }
                    )
                }
            }
        }

        class Factory(private val entryId: Long) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EntryTagsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return EntryTagsViewModel(entryId) as T
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
                Tags.ALL -> R.drawable.favicon_aggregator
                Tags.STARRED -> R.drawable.favicon_star
                Tags.PINNED -> R.drawable.favicon_pin
                else -> R.drawable.favicon_tag
            })

            textView.text = tag.name
            textView.isChecked = tag.newEntryTag
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
