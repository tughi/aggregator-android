package com.tughi.aggregator.activities.main

import android.database.Cursor
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.preferences.EntryListSettings

class TagEntriesFragment : EntriesFragment() {

    companion object {
        const val ARGUMENT_TAG_ID = "tag_id"

        fun newInstance(tagId: Long): TagEntriesFragment {
            return TagEntriesFragment().also {
                it.arguments = Bundle().apply {
                    putLong(ARGUMENT_TAG_ID, tagId)
                }
            }
        }
    }

    private val tagId by lazy { arguments!!.getLong(ARGUMENT_TAG_ID) }

    override val initialQueryCriteria: Entries.EntriesQueryCriteria
        get() = Entries.TagEntriesQueryCriteria(tagId = tagId, sortOrder = EntryListSettings.entriesSortOrder)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = TagEntriesViewModel.Factory(tagId)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(TagEntriesViewModel::class.java)
        viewModel.tag.observe(this, Observer { tag ->
            if (tag != null) {
                setTitle(tag.name)
            }
        })
    }

    override fun onNavigationClick() {
        fragmentManager?.popBackStack()
    }

    class Tag(val name: String) {
        object QueryHelper : Tags.QueryHelper<Tag>(
                Tags.NAME
        ) {
            override fun createRow(cursor: Cursor) = Tag(
                    name = cursor.getString(0)
            )
        }
    }

    class TagEntriesViewModel(tagId: Long) : ViewModel() {
        val tag = Tags.liveQueryOne(Tags.QueryTagCriteria(tagId), Tag.QueryHelper)

        class Factory(private val tagId: Long) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TagEntriesViewModel::class.java)) {
                    return TagEntriesViewModel(tagId) as T
                }
                throw IllegalArgumentException("Unsupported model class: $modelClass")
            }
        }
    }

}
