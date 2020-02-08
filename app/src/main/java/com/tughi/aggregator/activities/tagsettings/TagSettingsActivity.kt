package com.tughi.aggregator.activities.tagsettings

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
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.EntryTags
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.TagEntryRulesQueryCriteria
import com.tughi.aggregator.data.Tags
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TagSettingsActivity : AppActivity() {

    companion object {
        private const val EXTRA_TAG_ID = "tag_id"

        fun start(context: Context, tagId: Long?) {
            context.startActivity(
                    Intent(context, TagSettingsActivity::class.java).apply {
                        if (tagId != null) {
                            putExtra(EXTRA_TAG_ID, tagId)
                        }
                    }
            )
        }
    }

    private lateinit var viewModel: TagSettingsViewModel

    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = TagSettingsViewModel.Factory(intent.getLongExtra(EXTRA_TAG_ID, -1L).let { if (it == -1L) null else it })
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(TagSettingsViewModel::class.java)

        setContentView(R.layout.tag_settings_activity)

        val recyclerView = findViewById<RecyclerView>(R.id.list)
        recyclerView.adapter = adapter

        val saveButton = findViewById<Button>(R.id.save)
        saveButton.setOnClickListener { onSaveTag() }

        viewModel.tag.observe(this, Observer { tag ->
            if (tag != null) {
                invalidateOptionsMenu()
            }
        })

        viewModel.tagRules.observe(this, Observer { tagRules ->
            adapter.tagRules = tagRules
        })

        viewModel.manualTags.observe(this, Observer { manualTags ->
            adapter.userTags = manualTags
        })

        viewModel.newTagName.observe(this, Observer { newTagName ->
            val tagName = newTagName?.trim() ?: ""
            saveButton.isEnabled = tagName.isNotEmpty() && tagName != viewModel.tag.value?.name
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.tag_settings_activity, menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)

        if (menu != null) {
            val tag = viewModel.tag.value
            menu.findItem(R.id.delete)?.isVisible = tag?.deletable ?: false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.delete -> {
                viewModel.tag.value?.let { tag ->
                    DeleteTagDialogFragment.show(supportFragmentManager, tag.id, tag.name, true)
                }
                return true
            }
        }
        return false
    }

    private fun onSaveTag() {
        val tag = viewModel.tag.value
        val name = viewModel.newTagName.value ?: return

        GlobalScope.launch {
            if (tag != null) {
                Tags.update(
                        Tags.UpdateTagCriteria(tag.id),
                        Tags.NAME to name
                )
            } else {
                Tags.insert(
                        Tags.NAME to name
                )
            }
        }

        finish()
    }

    class Feed(
            val id: Long,
            val title: String
    ) {
        override fun toString() = title

        object QueryHelper : Feeds.QueryHelper<Feed>(
                Feeds.ID,
                Feeds.CUSTOM_TITLE,
                Feeds.TITLE
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                    id = cursor.getLong(0),
                    title = cursor.getString(1) ?: cursor.getString(2)
            )
        }
    }

    class Tag(val id: Long, val name: String, val deletable: Boolean) {
        object QueryHelper : Tags.QueryHelper<Tag>(
                Tags.ID,
                Tags.NAME,
                Tags.EDITABLE
        ) {
            override fun createRow(cursor: Cursor) = Tag(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    deletable = cursor.getInt(2) != 0
            )
        }
    }

    class TagRule(val id: Long, val feedTitle: String?, val condition: String, val taggedEntries: Int) {
        object QueryHelper : EntryTagRules.QueryHelper<TagRule>(
                EntryTagRules.ID,
                EntryTagRules.FEED_TITLE,
                EntryTagRules.CONDITION,
                EntryTagRules.TAGGED_ENTRIES
        ) {
            override fun createRow(cursor: Cursor) = TagRule(
                    id = cursor.getLong(0),
                    feedTitle = cursor.getString(1),
                    condition = cursor.getString(2),
                    taggedEntries = cursor.getInt(3)
            )
        }
    }

    class EntryTag(val entryId: Long) {
        object QueryHelper : EntryTags.QueryHelper<EntryTag>(
                EntryTags.ENTRY_ID
        ) {
            override fun createRow(cursor: Cursor) = EntryTag(
                    cursor.getLong(0)
            )
        }
    }

    class TagSettingsViewModel(tagId: Long?) : ViewModel() {

        val tag = MediatorLiveData<Tag>()

        val tagRules = EntryTagRules.liveQuery(TagEntryRulesQueryCriteria(tagId ?: 0), TagRule.QueryHelper)

        val manualTags = EntryTags.liveQueryCount(EntryTags.ManuallyTaggedEntriesQueryCriteria(tagId ?: 0), EntryTag.QueryHelper)

        var newTagName = MutableLiveData<String>()

        init {
            if (tagId != null) {
                val liveTag = Tags.liveQueryOne(Tags.QueryTagCriteria(tagId), Tag.QueryHelper)
                tag.addSource(liveTag) {
                    if (it != null) {
                        newTagName.value = it.name
                    }
                    tag.value = it
                    tag.removeSource(liveTag)
                }
            }
        }

        class Factory(private val tagId: Long?) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TagSettingsViewModel::class.java)) {
                    return TagSettingsViewModel(tagId) as T
                }
                throw IllegalArgumentException("Unsupported model class: $modelClass")
            }
        }

    }

    internal abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    internal inner class TagNameViewHolder(itemView: View) : ViewHolder(itemView) {
        val name = itemView.findViewById<EditText>(R.id.name).also {
            it.doAfterTextChanged { text ->
                viewModel.newTagName.value = text.toString()
            }
        }
    }

    internal class UserTagsViewHolder(itemView: View) : ViewHolder(itemView) {
        val countView = itemView.findViewById<TextView>(R.id.count)
    }

    internal class TagRuleViewHolder(itemView: View) : ViewHolder(itemView) {
        val textView = itemView.findViewById<TextView>(R.id.text)
        val countView = itemView.findViewById<TextView>(R.id.count)
    }

    internal inner class Adapter : RecyclerView.Adapter<ViewHolder>() {
        var tagRules: List<TagRule> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        var userTags: Int = 0
            set(value) {
                field = value
                notifyItemChanged(1)
            }

        override fun getItemCount(): Int = tagRules.size + 2

        override fun getItemViewType(position: Int): Int = when (position) {
            0 -> R.layout.tag_settings_activity__name
            1 -> R.layout.tag_settings_activity__user
            else -> R.layout.tag_settings_activity__rule
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
            return when (viewType) {
                R.layout.tag_settings_activity__name -> TagNameViewHolder(view)
                R.layout.tag_settings_activity__user -> UserTagsViewHolder(view)
                else -> TagRuleViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) = when (holder) {
            is TagNameViewHolder -> {
                holder.name.setText(viewModel.newTagName.value)
            }
            is UserTagsViewHolder -> {
                holder.countView.text = userTags.toString()
            }
            is TagRuleViewHolder -> {
                val tagRule = tagRules[position - 2]
                holder.countView.text = tagRule.taggedEntries.toString()
                holder.textView.text = when {
                    tagRule.feedTitle != null -> resources.getString(R.string.tag_settings__tagged_entries__rule__from_feed, tagRule.feedTitle)
                    else -> resources.getString(R.string.tag_settings__tagged_entries__rule__all_feeds)
                }
            }
            else -> {
                throw IllegalArgumentException("Unsupported holder type: ${holder.javaClass.name}")
            }
        }

    }

}
