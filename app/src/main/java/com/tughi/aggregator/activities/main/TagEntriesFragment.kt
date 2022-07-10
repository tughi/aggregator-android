package com.tughi.aggregator.activities.main

import android.database.Cursor
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.data.EntriesQueryCriteria
import com.tughi.aggregator.data.TagEntriesQueryCriteria
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

    private val tagId by lazy { requireArguments().getLong(ARGUMENT_TAG_ID) }

    override val initialQueryCriteria: EntriesQueryCriteria
        get() = when (tagId) {
            Tags.STARRED -> TagEntriesQueryCriteria(tagId = tagId, sessionTime = System.currentTimeMillis(), showRead = true, sortOrder = EntryListSettings.entriesSortOrder)
            else -> TagEntriesQueryCriteria(tagId = tagId, sessionTime = System.currentTimeMillis(), showRead = EntryListSettings.showReadEntries, sortOrder = EntryListSettings.entriesSortOrder)
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = TagEntriesViewModel.Factory(tagId)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(TagEntriesViewModel::class.java)
        viewModel.tag.observe(this, Observer { tag ->
            if (tag != null) {
                setTitle(tag.name)
            }
        })
    }

    override fun onNavigationClick() {
        parentFragmentManager.popBackStack()
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
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TagEntriesViewModel::class.java)) {
                    return TagEntriesViewModel(tagId) as T
                }
                throw IllegalArgumentException("Unsupported model class: $modelClass")
            }
        }
    }

}
