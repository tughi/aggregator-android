package com.tughi.aggregator.activities.entrytagrulesettings

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.database.getLongOrNull
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.App
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.feedspicker.FeedsPickerActivity
import com.tughi.aggregator.activities.optionpicker.Option
import com.tughi.aggregator.activities.optionpicker.OptionPickerActivity
import com.tughi.aggregator.activities.tagspicker.TagsPickerActivity
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.EntryTagRuleQueryCriteria
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.data.UpdateEntryTagRuleCriteria
import com.tughi.aggregator.entries.conditions.Condition
import com.tughi.aggregator.entries.conditions.ContentToken
import com.tughi.aggregator.entries.conditions.LinkToken
import com.tughi.aggregator.entries.conditions.StringToken
import com.tughi.aggregator.entries.conditions.TitleToken
import com.tughi.aggregator.services.EntryTagRuleHelper
import com.tughi.aggregator.widgets.DropDownButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntryTagRuleSettingsActivity : AppActivity() {

    companion object {
        private const val EXTRA_ENTRY_TAG_RULE_ID = "entry_tag_rule_id"
        private const val EXTRA_PRESET_FEED_ID = "preset_feed_id"
        private const val EXTRA_PRESET_TAG_ID = "preset_tag_id"

        private const val REQUEST_TYPE = 1
        private const val REQUEST_FEED = 2
        private const val REQUEST_TAGS = 3

        private const val TYPE_FEED = "feed"
        private const val TYPE_GLOBAL = "global"

        fun start(activity: Activity, entryTagRuleId: Long? = null, presetFeedId: Long? = null, presetTagId: Long? = null) {
            activity.startActivity(
                Intent(activity, EntryTagRuleSettingsActivity::class.java).apply {
                    if (entryTagRuleId != null) putExtra(EXTRA_ENTRY_TAG_RULE_ID, entryTagRuleId)
                    if (presetFeedId != null) putExtra(EXTRA_PRESET_FEED_ID, presetFeedId)
                    if (presetTagId != null) putExtra(EXTRA_PRESET_TAG_ID, presetTagId)
                }
            )
        }
    }

    private val viewModel: EntryTagRuleViewModel by lazy {
        val entryTagRuleId = if (intent.hasExtra(EXTRA_ENTRY_TAG_RULE_ID)) intent.getLongExtra(EXTRA_ENTRY_TAG_RULE_ID, 0) else null
        val presetFeedId = if (intent.hasExtra(EXTRA_PRESET_FEED_ID)) intent.getLongExtra(EXTRA_PRESET_FEED_ID, 0) else null
        val presetTagId = if (intent.hasExtra(EXTRA_PRESET_TAG_ID)) intent.getLongExtra(EXTRA_PRESET_TAG_ID, 0) else null

        val viewModelFactory = EntryTagRuleViewModel.Factory(entryTagRuleId, presetTagId, presetFeedId)
        ViewModelProvider(this, viewModelFactory).get(EntryTagRuleViewModel::class.java)
    }

    private lateinit var typeView: DropDownButton
    private lateinit var feedView: DropDownButton
    private lateinit var tagView: DropDownButton
    private lateinit var conditionView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_back)
        }

        setContentView(R.layout.entry_tag_rule_settings_activity)

        typeView = findViewById(R.id.type)
        typeView.setOnClickListener {
            val options = arrayOf(
                Option(TYPE_FEED, getString(R.string.entry_tag_rule__type__feed), getString(R.string.entry_tag_rule__type__feed__description)),
                Option(TYPE_GLOBAL, getString(R.string.entry_tag_rule__type__global), getString(R.string.entry_tag_rule__type__global__description))
            )
            val selectedOption = options[if (viewModel.newType.value == TYPE_FEED) 0 else 1]
            OptionPickerActivity.startForResult(this, REQUEST_TYPE, options, selectedOption, titleResource = R.string.entry_tag_rule__type__title)
        }

        feedView = findViewById(R.id.feed)
        feedView.setOnClickListener {
            val feedId = viewModel.newFeedId.value
            val selectedFeeds = if (feedId != null) longArrayOf(feedId) else longArrayOf()
            FeedsPickerActivity.startForResult(this, REQUEST_FEED, selectedFeeds, true, null)
        }

        tagView = findViewById(R.id.tag)
        tagView.setOnClickListener {
            TagsPickerActivity.startForResult(this, REQUEST_TAGS, selectedTags = longArrayOf(viewModel.newTagId.value ?: 0), singleChoice = true)
        }

        val saveButton = findViewById<Button>(R.id.save)
        saveButton.isEnabled = false
        saveButton.setOnClickListener {
            viewModel.save()
            finish()
        }

        conditionView = findViewById(R.id.condition)
        conditionView.doAfterTextChanged {
            if (it != null && it.isNotEmpty()) {
                val condition = Condition(it)

                it.getSpans(0, it.length, ForegroundColorSpan::class.java).forEach { span -> it.removeSpan(span) }
                it.getSpans(0, it.length, UnderlineSpan::class.java).forEach { span -> it.removeSpan(span) }

                condition.tokens.forEach { token ->
                    when {
                        token.isUnexpected -> {
                            it.setSpan(UnderlineSpan(), token.startIndex, token.endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        token is StringToken || token is TitleToken || token is ContentToken || token is LinkToken -> {
                            it.setSpan(ForegroundColorSpan(App.accentColor), token.startIndex, token.endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                }

                viewModel.newCondition.value = condition
            } else {
                viewModel.newCondition.value = Condition("")
            }
        }

        viewModel.newType.observe(this, Observer { type ->
            if (type == TYPE_FEED) {
                typeView.setText(R.string.entry_tag_rule__type__feed)
                feedView.visibility = View.VISIBLE
            } else {
                typeView.setText(R.string.entry_tag_rule__type__global)
                feedView.visibility = View.GONE
            }
            saveButton.isEnabled = viewModel.canSave
        })

        viewModel.newFeed.observe(this, Observer { feed ->
            feedView.setText(feed?.title ?: "")
            saveButton.isEnabled = viewModel.canSave
        })

        viewModel.newTag.observe(this, Observer { tag ->
            if (tag != null) {
                tagView.setText(tag.name)
            }
            saveButton.isEnabled = viewModel.canSave
        })

        viewModel.oldCondition.observe(this, Observer { condition ->
            conditionView.setText(condition.toString())
        })
        viewModel.newCondition.observe(this, Observer { text ->
            saveButton.isEnabled = viewModel.canSave
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_FEED -> {
                    val selectedFeeds = data?.getLongArrayExtra(FeedsPickerActivity.EXTRA_SELECTED_FEEDS) ?: longArrayOf()
                    viewModel.newFeedId.value = if (selectedFeeds.isNotEmpty()) selectedFeeds[0] else null
                    return
                }
                REQUEST_TAGS -> {
                    val selectedTags = data?.getLongArrayExtra(TagsPickerActivity.EXTRA_SELECTED_TAGS) ?: return
                    viewModel.newTagId.value = selectedTags[0]
                    return
                }
                REQUEST_TYPE -> {
                    val selectedOption: Option = data?.getParcelableExtra(OptionPickerActivity.EXTRA_SELECTED_OPTION) ?: return
                    viewModel.newType.value = selectedOption.value
                    return
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.entry_tag_rule_settings_activity, menu)
        menu.findItem(R.id.delete).isVisible = viewModel.entryTagRuleId != null

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                viewModel.entryTagRuleId?.let {
                    DeleteEntryTagRuleDialogFragment.show(supportFragmentManager, it, true)
                }
            }
            android.R.id.home -> {
                finish()
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    internal class EntryTagRule(
        val id: Long,
        val feedId: Long?,
        val tagId: Long,
        val condition: String
    ) {
        object QueryHelper : EntryTagRules.QueryHelper<EntryTagRule>(
            EntryTagRules.ID,
            EntryTagRules.FEED_ID,
            EntryTagRules.TAG_ID,
            EntryTagRules.CONDITION
        ) {
            override fun createRow(cursor: Cursor) = EntryTagRule(
                cursor.getLong(0),
                cursor.getLongOrNull(1),
                cursor.getLong(2),
                cursor.getString(3)
            )
        }
    }

    internal class Feed(
        val title: String
    ) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
            Feeds.TITLE,
            Feeds.CUSTOM_TITLE
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                cursor.getString(1) ?: cursor.getString(0)
            )
        }
    }

    internal class Tag(
        val id: Long,
        val name: String
    ) {
        object QueryHelper : Tags.QueryHelper<Tag>(
            Tags.ID,
            Tags.NAME
        ) {
            override fun createRow(cursor: Cursor) = Tag(
                cursor.getLong(0),
                cursor.getString(1)
            )
        }
    }

    internal class EntryTagRuleViewModel(val entryTagRuleId: Long?, presetTagId: Long?, presetFeedId: Long?) : ViewModel() {
        private var entryTagRule: EntryTagRule? = null

        val newType = MutableLiveData<String>()

        val newFeedId = MutableLiveData<Long?>()
        val newFeed = Transformations.switchMap(newFeedId) { newFeedId ->
            Feeds.liveQueryOne(Feeds.QueryRowCriteria(newFeedId ?: 0), Feed.QueryHelper)
        }

        val newTagId = MutableLiveData<Long?>()
        val newTag = Transformations.switchMap(newTagId) { newTagId ->
            Tags.liveQueryOne(Tags.QueryTagCriteria(newTagId ?: 0), Tag.QueryHelper)
        }

        val oldCondition = MutableLiveData<Condition>()
        val newCondition = MutableLiveData<Condition>()

        init {
            if (entryTagRuleId != null) {
                contentScope.launch {
                    val entryTagRule = EntryTagRules.queryOne(EntryTagRuleQueryCriteria(entryTagRuleId), EntryTagRule.QueryHelper)
                    if (entryTagRule != null) {
                        launch(Dispatchers.Main) {
                            this@EntryTagRuleViewModel.entryTagRule = entryTagRule
                            newType.value = if (entryTagRule.feedId != null) TYPE_FEED else TYPE_GLOBAL
                            newFeedId.value = entryTagRule.feedId
                            newTagId.value = entryTagRule.tagId
                            oldCondition.value = Condition(entryTagRule.condition)
                        }
                    }
                }
            } else {
                newType.value = TYPE_FEED
                newFeedId.value = presetFeedId
                newTagId.value = presetTagId
                oldCondition.value = Condition("")
            }
        }

        val canSave: Boolean
            get() {
                val newTagId = newTagId.value ?: return false
                val newType = newType.value ?: return false
                val newFeedId = newFeedId.value ?: if (newType == TYPE_FEED) return false else null
                val newCondition = newCondition.value ?: return false
                if (newCondition.hasUnexpectedTokens) return false
                val entryTagRule = entryTagRule ?: return true
                if (entryTagRule.tagId != newTagId) return true
                if (newType == TYPE_FEED && entryTagRule.feedId != newFeedId) return true
                if (newType == TYPE_GLOBAL && entryTagRule.feedId != null) return true
                if (oldCondition.value.toString() != newCondition.toString()) return true
                return false
            }

        fun save() {
            val entryTagRuleId = entryTagRuleId
            val feedId = if (newType.value == TYPE_FEED) newFeedId.value else null
            val tagId = newTagId.value ?: return
            val condition = newCondition.value ?: return

            contentScope.launch {
                if (entryTagRuleId != null) {
                    EntryTagRules.update(
                        UpdateEntryTagRuleCriteria(entryTagRuleId),
                        EntryTagRules.FEED_ID to feedId,
                        EntryTagRules.TAG_ID to tagId,
                        EntryTagRules.CONDITION to condition.toString()
                    )
                    EntryTagRuleHelper.apply(entryTagRuleId, deleteOldTags = true)
                } else {
                    val id = EntryTagRules.insert(
                        EntryTagRules.FEED_ID to feedId,
                        EntryTagRules.TAG_ID to tagId,
                        EntryTagRules.CONDITION to condition.toString()
                    )
                    EntryTagRuleHelper.apply(id)
                }
            }
        }

        class Factory(private val entryTagRuleId: Long?, private val presetTagId: Long?, private val presetFeedId: Long?) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EntryTagRuleViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return EntryTagRuleViewModel(entryTagRuleId, presetTagId, presetFeedId) as T
                }
                throw UnsupportedOperationException()
            }
        }
    }

}
