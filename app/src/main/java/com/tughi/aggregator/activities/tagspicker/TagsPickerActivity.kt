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
import android.widget.Button
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
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.tagsettings.TagSettingsActivity
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.utilities.has

class TagsPickerActivity : AppActivity() {

    companion object {
        const val EXTRA_SELECTED_TAGS = "selected_tags"
        private const val EXTRA_SINGLE_CHOICE = "single_choice"
        private const val EXTRA_TITLE = "title"

        fun startForResult(activity: Activity, resultCode: Int, selectedTags: LongArray, singleChoice: Boolean, title: String? = null) {
            activity.startActivityForResult(
                    Intent(activity, TagsPickerActivity::class.java)
                            .putExtra(EXTRA_SELECTED_TAGS, selectedTags)
                            .putExtra(EXTRA_SINGLE_CHOICE, singleChoice)
                            .putExtra(EXTRA_TITLE, title),
                    resultCode
            )
        }

        fun startForResult(fragment: Fragment, resultCode: Int, selectedTags: LongArray, title: String? = null) {
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

    private val singleChoice by lazy { intent.getBooleanExtra(EXTRA_SINGLE_CHOICE, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_back)
        }

        intent.getStringExtra(EXTRA_TITLE)?.let {
            title = it
        }

        val selectedTagIds = intent.getLongArrayExtra(EXTRA_SELECTED_TAGS) ?: LongArray(0)
        val viewModelFactory = TagsPickerViewModel.Factory(selectedTagIds, singleChoice)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(TagsPickerViewModel::class.java)

        setContentView(R.layout.tags_picker_activity)
        val recyclerView = findViewById<RecyclerView>(R.id.list)
        val progressBar = findViewById<ProgressBar>(R.id.progress)

        val adapter = TagsAdapter(object : TagsAdapter.Listener {
            override fun onTagClick(tag: Tag) {
                viewModel.toggleTag(tag.id)
            }
        }, singleChoice)

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

        val selectButton = findViewById<Button>(R.id.select)
        selectButton.setOnClickListener {
            val selectedTags = viewModel.tags.value?.filter { it.selected } ?: emptyList()
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_SELECTED_TAGS, LongArray(selectedTags.size) { selectedTags[it].id }))

            finish()
        }
        if (singleChoice) {
            selectButton.isEnabled = false
            viewModel.tags.observe(this, Observer { _ ->
                selectButton.isEnabled = viewModel.tags.value?.has { it.selected } ?: false
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.tags_picker_fragment, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
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

    class TagsPickerViewModel(initialSelectedTagIds: LongArray, private val singleChoice: Boolean) : ViewModel() {
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
                if (singleChoice) {
                    selectedTags.clear()
                    selectedTags.put(tagId, true)
                } else {
                    selectedTags.put(tagId, !selectedTags.get(tagId, false))
                }
                value = selectedTags
            }
        }

        class Factory(private val selectedTagIds: LongArray, private val singleChoice: Boolean) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TagsPickerViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return TagsPickerViewModel(selectedTagIds, singleChoice) as T
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
            textView.isChecked = tag.selected
        }
    }

    class TagsAdapter(private val listener: Listener, private val singleChoice: Boolean) : RecyclerView.Adapter<TagViewHolder>() {
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
                LayoutInflater.from(parent.context).inflate(if (singleChoice) R.layout.tags_picker_item__single_choice else R.layout.tags_picker_item__multiple_choice, parent, false),
                listener
        )

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) = holder.onBind(tags[position])

        interface Listener {
            fun onTagClick(tag: Tag)
        }
    }

}
