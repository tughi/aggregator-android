package com.tughi.aggregator.activities.tagsettings

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.App
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.entrytagrulesettings.EntryTagRuleSettingsActivity
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.EntryTags
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.TagEntryRulesQueryCriteria
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.entries.conditions.Condition
import com.tughi.aggregator.entries.conditions.ContentToken
import com.tughi.aggregator.entries.conditions.LinkToken
import com.tughi.aggregator.entries.conditions.StringToken
import com.tughi.aggregator.entries.conditions.TitleToken
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
        viewModel = ViewModelProvider(this, viewModelFactory).get(TagSettingsViewModel::class.java)

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.tag_settings_activity, menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        val tag = viewModel.tag.value
        menu.findItem(R.id.delete)?.isVisible = tag?.deletable ?: false
        menu.findItem(R.id.add_rule)?.isVisible = tag != null && tag.id != Tags.ALL

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_rule -> {
                EntryTagRuleSettingsActivity.start(this, presetTagId = viewModel.tagId)
                return true
            }
            R.id.delete -> {
                viewModel.tag.value?.let { tag ->
                    DeleteTagDialogFragment.show(supportFragmentManager, tag.id, tag.name, true)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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

    class TagRule(val id: Long, val feedTitle: String?, val condition: Condition, val taggedEntries: Int) {
        val label: CharSequence

        init {
            val hasFeedTitle = feedTitle != null
            val hasCondition = condition.tokens.size > 1
            val text = SpannableStringBuilder(
                App.instance.getString(
                    when {
                        hasFeedTitle && hasCondition -> R.string.tag_settings__tagged_entries__rule__from_feed_with_condition
                        hasFeedTitle -> R.string.tag_settings__tagged_entries__rule__from_feed
                        hasCondition -> R.string.tag_settings__tagged_entries__rule__all_feeds_with_condition
                        else -> R.string.tag_settings__tagged_entries__rule__all_feeds
                    }
                )
            )
            if (hasFeedTitle) {
                val feedTitle = SpannableString(feedTitle).also {
                    it.setSpan(ForegroundColorSpan(App.accentColor), 0, it.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                val feedTitleIndex = text.indexOf("\$FEED_TITLE")
                text.replace(feedTitleIndex, feedTitleIndex + 11, feedTitle)
            }
            if (hasCondition) {
                val condition = SpannableString(condition.text).also {
                    condition.tokens.forEach { token ->
                        when (token) {
                            is StringToken, is TitleToken, is ContentToken, is LinkToken -> {
                                it.setSpan(ForegroundColorSpan(App.accentColor), token.startIndex, token.endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                        }
                    }
                }
                val conditionIndex = text.indexOf("\$CONDITION")
                text.replace(conditionIndex, conditionIndex + 10, condition)
            }
            label = text
        }

        object QueryHelper : EntryTagRules.QueryHelper<TagRule>(
            EntryTagRules.ID,
            EntryTagRules.FEED_TITLE,
            EntryTagRules.CONDITION,
            EntryTagRules.TAGGED_ENTRIES
        ) {
            override fun createRow(cursor: Cursor) = TagRule(
                id = cursor.getLong(0),
                feedTitle = cursor.getString(1),
                condition = Condition(cursor.getString(2)),
                taggedEntries = cursor.getInt(3)
            )
        }
    }

    class TagSettingsViewModel(val tagId: Long?) : ViewModel() {

        val tag = MutableLiveData<Tag>()
        val manualTags = MutableLiveData<Int>()

        val tagRules = EntryTagRules.liveQuery(TagEntryRulesQueryCriteria(tagId ?: 0), TagRule.QueryHelper)

        var newTagName = MutableLiveData<String>()

        init {
            if (tagId != null) {
                GlobalScope.launch {
                    Tags.queryOne(Tags.QueryTagCriteria(tagId), Tag.QueryHelper)?.let {
                        tag.postValue(it)
                        newTagName.postValue(it.name)
                    }
                }
                GlobalScope.launch {
                    manualTags.postValue(
                        EntryTags.queryCount(
                            EntryTags.ManuallyTaggedEntriesQueryCriteria(tagId),
                            object : EntryTags.QueryHelper<Any>(EntryTags.ENTRY_ID) {
                                override fun createRow(cursor: Cursor) = Unit
                            }
                        )
                    )
                }
            }
        }

        class Factory(private val tagId: Long?) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
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

    internal inner class TagRuleViewHolder(itemView: View) : ViewHolder(itemView) {
        val textView = itemView.findViewById<TextView>(R.id.text)
        val countView = itemView.findViewById<TextView>(R.id.count)

        var tagRule: TagRule? = null

        init {
            itemView.setOnClickListener {
                val tagRule = tagRule
                if (tagRule != null) {
                    EntryTagRuleSettingsActivity.start(this@TagSettingsActivity, tagRule.id)
                }
            }
        }
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

        override fun getItemCount(): Int = when (viewModel.tagId) {
            null, Tags.ALL -> 1
            else -> tagRules.size + 2
        }

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
                holder.tagRule = tagRule
                holder.textView.text = tagRule.label
                holder.countView.text = tagRule.taggedEntries.toString()
            }
            else -> {
                throw IllegalArgumentException("Unsupported holder type: ${holder.javaClass.name}")
            }
        }

    }

}
