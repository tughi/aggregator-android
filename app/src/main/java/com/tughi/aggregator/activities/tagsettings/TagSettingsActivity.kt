package com.tughi.aggregator.activities.tagsettings

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Feeds
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

    private val nameTextView by lazy { findViewById<TextView>(R.id.name) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = TagSettingsViewModel.Factory(intent.getLongExtra(EXTRA_TAG_ID, -1L).let { if (it == -1L) null else it })
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(TagSettingsViewModel::class.java)

        setContentView(R.layout.tag_settings_activity)

        findViewById<Button>(R.id.save).setOnClickListener { onSaveTag() }

        viewModel.tag.observe(this, Observer { tag ->
            if (tag != null) {
                invalidateOptionsMenu()

                nameTextView.text = tag.name
            }
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
        val name = nameTextView.text.toString().trim()

        GlobalScope.launch {
            Database.transaction {
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

    class TagSettingsViewModel(tagId: Long?) : ViewModel() {
        val tag = MediatorLiveData<Tag>()

        init {
            if (tagId != null) {
                val liveTag = Tags.liveQueryOne(Tags.QueryTagCriteria(tagId), Tag.QueryHelper)
                tag.addSource(liveTag) {
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


}
