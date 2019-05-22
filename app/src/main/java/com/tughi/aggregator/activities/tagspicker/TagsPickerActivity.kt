package com.tughi.aggregator.activities.tagspicker

import android.app.Activity
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
import androidx.collection.LongSparseArray
import androidx.fragment.app.Fragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.tagsettings.TagSettingsActivity
import com.tughi.aggregator.data.Tags

class TagsPickerActivity : AppActivity() {

    companion object {
        const val EXTRA_SELECTED_TAGS = "selected_tags"
        private const val EXTRA_TITLE = "title"

        fun startForResult(fragment: Fragment, resultCode: Int, selectedTags: LongArray, title: String?) {
            fragment.context?.let { context ->
                fragment.startActivityForResult(
                        Intent(context, TagsPickerActivity::class.java)
                                .putExtra(EXTRA_SELECTED_TAGS, selectedTags)
                                .putExtra(EXTRA_TITLE, title),
                        resultCode
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra(EXTRA_TITLE)?.let {
            title = it
        }

        val selectedTagIds = intent.getLongArrayExtra(EXTRA_SELECTED_TAGS) ?: LongArray(0)
        val viewModelFactory = FeedTagsViewModel.Factory(selectedTagIds)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(FeedTagsViewModel::class.java)

        setContentView(R.layout.simple_list)
        val recyclerView = findViewById<RecyclerView>(R.id.list)
        val progressBar = findViewById<ProgressBar>(R.id.progress)

        val adapter = TagsAdapter(object : TagsAdapter.Listener {
            override fun onTagClick(tag: Tag) {
                viewModel.toggleTag(tag.id)

                val selectedTags = viewModel.tags.value?.filter { it.selected } ?: emptyList()
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_SELECTED_TAGS, LongArray(selectedTags.size) { selectedTags[it].id }))
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

        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_SELECTED_TAGS, selectedTagIds))
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

    data class Tag(val id: Long, val name: String, val selected: Boolean = false) {
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

    class FeedTagsViewModel(initialSelectedTagIds: LongArray) : ViewModel() {
        private val selectedTagIds = MutableLiveData<LongSparseArray<Boolean>>().apply {
            val selectedTags = LongSparseArray<Boolean>()
            for (selectedTag in initialSelectedTagIds) {
                selectedTags.put(selectedTag, true)
            }
            value = selectedTags
        }

        val tags = MediatorLiveData<List<Tag>>()

        init {
            val tags = Tags.liveQuery(Tags.QueryUserTagsCriteria, Tag.QueryHelper)

            this.tags.addSource(this.selectedTagIds) { mergeTags(tags.value, it) }
            this.tags.addSource(tags) { mergeTags(it, this.selectedTagIds.value) }
        }

        private fun mergeTags(tags: List<Tag>?, selectedTags: LongSparseArray<Boolean>?) {
            this.tags.value = when {
                tags == null || selectedTags == null -> emptyList()
                selectedTags.isEmpty -> tags
                else -> tags.map { tag ->
                    tag.copy(
                            selected = selectedTags.get(tag.id, false)
                    )
                }
            }
        }

        fun toggleTag(tagId: Long) {
            selectedTagIds.apply {
                val selectedTags = value ?: LongSparseArray()
                selectedTags.put(tagId, !selectedTags.get(tagId, false))
                value = selectedTags
            }
        }

        class Factory(private val selectedTagIds: LongArray) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FeedTagsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return FeedTagsViewModel(selectedTagIds) as T
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
            textView.isChecked = tag.selected
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
