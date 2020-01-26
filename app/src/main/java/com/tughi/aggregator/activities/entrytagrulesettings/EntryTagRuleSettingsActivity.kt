package com.tughi.aggregator.activities.entrytagrulesettings

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.core.database.getLongOrNull
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.optionpicker.Option
import com.tughi.aggregator.activities.optionpicker.OptionPickerActivity
import com.tughi.aggregator.activities.tagspicker.TagsPickerActivity
import com.tughi.aggregator.data.EntryTagRuleQueryCriteria
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.data.UpdateEntryTagRuleCriteria
import com.tughi.aggregator.widgets.DropDownButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EntryTagRuleSettingsActivity : AppActivity() {

    companion object {
        private const val EXTRA_ENTRY_TAG_RULE_ID = "entry_tag_rule_id"
        private const val EXTRA_FEED_ID = "feed_id"

        private const val REQUEST_ENTRY_TAG_RULE_TYPE = 1
        private const val REQUEST_TAGS = 2

        fun start(activity: Activity, entryTagRuleId: Long?, feedId: Long) {
            activity.startActivity(
                    Intent(activity, EntryTagRuleSettingsActivity::class.java)
                            .putExtra(EXTRA_ENTRY_TAG_RULE_ID, entryTagRuleId)
                            .putExtra(EXTRA_FEED_ID, feedId)
            )
        }
    }

    private val entryTagRuleId: Long? by lazy {
        intent.getLongExtra(EXTRA_ENTRY_TAG_RULE_ID, 0).let {
            if (it != 0L) {
                it
            } else {
                null
            }
        }
    }
    private val feedId: Long by lazy {
        intent.getLongExtra(EXTRA_FEED_ID, 0)
    }
    private val viewModel: EntryTagRuleViewModel by lazy {
        val viewModelFactory = EntryTagRuleViewModel.Factory(entryTagRuleId, feedId)
        ViewModelProviders.of(this, viewModelFactory).get(EntryTagRuleViewModel::class.java)
    }

    private lateinit var typeView: DropDownButton
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
                    Option("feed", getString(R.string.entry_tag_rule__type__feed), getString(R.string.entry_tag_rule__type__feed__description, viewModel.feedTitle)),
                    Option("global", getString(R.string.entry_tag_rule__type__global), getString(R.string.entry_tag_rule__type__global__description))
            )
            val selectedOption = if (viewModel.newFeedId.value == feedId) options[0] else options[1]
            OptionPickerActivity.startForResult(this, REQUEST_ENTRY_TAG_RULE_TYPE, options, selectedOption, titleResource = R.string.entry_tag_rule__type__title)
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

        viewModel.newFeedId.observe(this, Observer { feedId ->
            if (feedId != null) {
                typeView.setText(R.string.entry_tag_rule__type__feed)
            } else {
                typeView.setText(R.string.entry_tag_rule__type__global)
            }
        })

        viewModel.newTag.observe(this, Observer { tag ->
            if (tag != null) {
                tagView.setText(tag.name)
            }
        })

        viewModel.validated.observe(this, Observer { validated ->
            saveButton.isEnabled = validated
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_ENTRY_TAG_RULE_TYPE -> {
                    val selectedOption: Option = data?.getParcelableExtra(OptionPickerActivity.EXTRA_SELECTED_OPTION) ?: return
                    viewModel.newFeedId.value = if (selectedOption.value == "feed") viewModel.feedId else null
                    return
                }
                REQUEST_TAGS -> {
                    val selectedTags = data?.getLongArrayExtra(TagsPickerActivity.EXTRA_SELECTED_TAGS) ?: return
                    viewModel.newTagId.value = selectedTags[0]
                    return
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        if (menu != null) {
            menuInflater.inflate(R.menu.entry_tag_rule_settings_activity, menu)
            menu.findItem(R.id.delete).isVisible = entryTagRuleId != null
        }

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

    internal class EntryTagRuleViewModel(val entryTagRuleId: Long?, val feedId: Long) : ViewModel() {
        var feedTitle: String = ""

        val newFeedId = MutableLiveData<Long>()

        val newTagId = MutableLiveData<Long>()
        val newTag = Transformations.switchMap(newTagId) { newTagId ->
            Tags.liveQueryOne(Tags.QueryTagCriteria(newTagId), Tag.QueryHelper)
        }

        init {
            GlobalScope.launch {
                Feeds.queryOne(Feeds.QueryRowCriteria(feedId), Feed.QueryHelper)?.also {
                    feedTitle = it.title
                }
            }
            if (entryTagRuleId != null) {
                GlobalScope.launch {
                    val entryTagRule = EntryTagRules.queryOne(EntryTagRuleQueryCriteria(entryTagRuleId), EntryTagRule.QueryHelper)
                    if (entryTagRule != null) {
                        launch(Dispatchers.Main) {
                            newFeedId.value = if (entryTagRule.feedId == feedId) feedId else null
                            newTagId.value = entryTagRule.tagId
                        }
                    }
                }
            } else {
                newFeedId.value = feedId
            }
        }

        private val invalid = MutableLiveData<Boolean>().also { it.value = false }
        private val valid = MutableLiveData<Boolean>().also { it.value = true }

        val validated = Transformations.switchMap(newTag) { tag ->
            if (tag != null) {
                valid
            } else {
                invalid
            }
        }

        fun save() {
            val entryTagRuleId = entryTagRuleId
            val feedId = newFeedId.value
            val tagId = newTagId.value ?: return
            val condition = ""

            GlobalScope.launch {
                if (entryTagRuleId != null) {
                    EntryTagRules.update(
                            UpdateEntryTagRuleCriteria(entryTagRuleId),
                            EntryTagRules.FEED_ID to feedId,
                            EntryTagRules.TAG_ID to tagId,
                            EntryTagRules.CONDITION to condition
                    )
                    // TODO: delete existing tags
                } else {
                    EntryTagRules.insert(
                            EntryTagRules.FEED_ID to feedId,
                            EntryTagRules.TAG_ID to tagId,
                            EntryTagRules.CONDITION to condition
                    )
                }
                // TODO: run this rule
            }
        }

        class Factory(private val entryTagRuleId: Long?, private val feedId: Long) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EntryTagRuleViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return EntryTagRuleViewModel(entryTagRuleId, feedId) as T
                }
                throw UnsupportedOperationException()
            }
        }
    }

}
