package com.tughi.aggregator.activities.entrytagrule

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
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
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.widgets.DropDownButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EntryTagRuleActivity : AppActivity() {

    companion object {
        private const val EXTRA_FEED_ID = "feed_id"

        private const val REQUEST_ENTRY_TAG_RULE_TYPE = 1
        private const val REQUEST_TAGS = 2

        fun start(activity: Activity, feedId: Long) {
            activity.startActivity(
                    Intent(activity, EntryTagRuleActivity::class.java)
                            .putExtra(EXTRA_FEED_ID, feedId)
            )
        }


        private val TYPE_OPTION_FEED = Option("feed-type", R.string.entry_tag_rule__type__feed, R.string.entry_tag_rule__type__feed__description)
        private val TYPE_OPTION_GLOBAL = Option("global-type", R.string.entry_tag_rule__type__global, R.string.entry_tag_rule__type__global__description)
        private val TYPE_OPTIONS = arrayOf(TYPE_OPTION_FEED, TYPE_OPTION_GLOBAL)
    }

    private val feedId: Long by lazy { intent.getLongExtra(EXTRA_FEED_ID, 0) }
    private val viewModel: EntryTagRuleViewModel by lazy {
        val viewModelFactory = EntryTagRuleViewModel.Factory(feedId)
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

        setContentView(R.layout.entry_tag_rule_activity)

        typeView = findViewById(R.id.type)
        typeView.setOnClickListener {
            OptionPickerActivity.startForResult(this, REQUEST_ENTRY_TAG_RULE_TYPE, TYPE_OPTIONS, viewModel.newTypeOption.value, titleResource = R.string.entry_tag_rule__type__title)
        }

        tagView = findViewById(R.id.tag)
        tagView.setOnClickListener {
            TagsPickerActivity.startForResult(this, REQUEST_TAGS, selectedTags = longArrayOf(viewModel.newTagId.value ?: 0), singleChoice = true)
        }

        val saveButton = findViewById<Button>(R.id.save)
        saveButton.isEnabled = false
        saveButton.setOnClickListener {
            viewModel.save()
        }

        conditionView = findViewById(R.id.condition)

        viewModel.newTypeOption.observe(this, Observer { type ->
            if (type != null) {
                typeView.setText(type.name)
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
                    viewModel.newTypeOption.value = selectedOption
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    class EntryTagRuleViewModel(val feedId: Long) : ViewModel() {
        val newTypeOption = MutableLiveData<Option>()

        val newTagId = MutableLiveData<Long>()
        val newTag = Transformations.switchMap(newTagId) { newTagId ->
            Tags.liveQueryOne(Tags.QueryTagCriteria(newTagId), Tag.QueryHelper)
        }

        private val invalid = MutableLiveData<Boolean>().also { it.value = false }
        private val valid = MutableLiveData<Boolean>().also { it.value = true }

        val validated = Transformations.switchMap(newTag) { tag ->
            if (tag != null) {
                Transformations.switchMap(newTypeOption) { type ->
                    if (type != null) {
                        valid
                    } else {
                        invalid
                    }
                }
            } else {
                invalid
            }
        }

        fun save() {
            val feedId = if (newTypeOption.value == TYPE_OPTION_GLOBAL) null else feedId
            val tagId = newTagId.value ?: return
            val condition = ""

            GlobalScope.launch {
                EntryTagRules.insert(
                        EntryTagRules.FEED_ID to feedId,
                        EntryTagRules.TAG_ID to tagId,
                        EntryTagRules.CONDITION to condition
                )
            }
        }

        class Factory(private val feedId: Long) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EntryTagRuleViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return EntryTagRuleViewModel(feedId) as T
                }
                throw UnsupportedOperationException()
            }
        }
    }

    class EntryTagRule(
            val id: Long,
            val tagName: String
    ) {
        object QueryHelper : EntryTagRules.QueryHelper<EntryTagRule>(
                EntryTagRules.ID,
                EntryTagRules.TAG_NAME
        ) {
            override fun createRow(cursor: Cursor) = EntryTagRule(
                    cursor.getLong(0),
                    cursor.getString(1)
            )
        }
    }

    class Tag(
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

}
