package com.tughi.aggregator.activities.myfeedsettings

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.tagspicker.TagsPickerActivity
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.MyFeedTags
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.widgets.DropDownButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyFeedSettingsFragment : Fragment() {

    private val viewModel by lazy { ViewModelProviders.of(this).get(MyFeedSettingsViewModel::class.java) }

    private lateinit var includedTags: DropDownButton
    private lateinit var excludedTags: DropDownButton

    companion object {
        private const val REQUEST_INCLUDED_TAGS = 1
        private const val REQUEST_EXCLUDED_TAGS = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.my_feed_settings_fragment, container, false)

        includedTags = fragmentView.findViewById(R.id.included_tags)
        includedTags.setOnClickListener {
            val includedTagIds = viewModel.newIncludedTagIds.value ?: LongArray(0)
            TagsPickerActivity.startForResult(this, REQUEST_INCLUDED_TAGS, includedTagIds, getString(R.string.my_feed_settings__included_tags))
        }
        viewModel.includedTags.observe(this, Observer { tags ->
            if (tags.isNullOrEmpty()) {
                includedTags.setText(R.string.my_feed_settings__included_tags__none)
            } else {
                includedTags.setText(tags.joinToString())
            }
        })

        excludedTags = fragmentView.findViewById(R.id.excluded_tags)
        excludedTags.setOnClickListener {
            val excludedTagIds = viewModel.newExcludedTagIds.value ?: LongArray(0)
            TagsPickerActivity.startForResult(this, REQUEST_EXCLUDED_TAGS, excludedTagIds, getString(R.string.my_feed_settings__excluded_tags))
        }
        viewModel.excludedTags.observe(this, Observer { tags ->
            if (tags.isNullOrEmpty()) {
                excludedTags.setText(R.string.my_feed_settings__excluded_tags__none)
            } else {
                excludedTags.setText(tags.joinToString())
            }
        })

        return fragmentView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_INCLUDED_TAGS -> {
                    val selectedTags = data?.getLongArrayExtra(TagsPickerActivity.EXTRA_SELECTED_TAGS) ?: return
                    viewModel.newIncludedTagIds.value = selectedTags
                }
                REQUEST_EXCLUDED_TAGS -> {
                    val selectedTags = data?.getLongArrayExtra(TagsPickerActivity.EXTRA_SELECTED_TAGS) ?: return
                    viewModel.newExcludedTagIds.value = selectedTags
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater?.inflate(R.menu.my_feed_settings_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.save -> onSave()
        else -> super.onOptionsItemSelected(item)
    }

    private fun onSave(): Boolean {
        val oldIncludedTagIds = viewModel.oldIncludedTagIds
        val newIncludedTagIds = viewModel.newIncludedTagIds.value ?: LongArray(0)
        val oldExcludedTagIds = viewModel.oldExcludedTagIds
        val newExcludedTagIds = viewModel.newExcludedTagIds.value ?: LongArray(0)

        GlobalScope.launch {
            Database.transaction {
                for (tagId in newIncludedTagIds) {
                    if (!oldIncludedTagIds.contains(tagId)) {
                        MyFeedTags.insert(
                                MyFeedTags.TAG_ID to tagId,
                                MyFeedTags.TYPE to MyFeedTags.Type.INCLUDED.value
                        )
                    }
                }

                for (tagId in oldIncludedTagIds) {
                    if (!newIncludedTagIds.contains(tagId)) {
                        MyFeedTags.delete(MyFeedTags.DeleteMyFeedTagCriteria(tagId, MyFeedTags.Type.INCLUDED))
                    }
                }

                for (tagId in newExcludedTagIds) {
                    if (!oldExcludedTagIds.contains(tagId)) {
                        MyFeedTags.insert(
                                MyFeedTags.TAG_ID to tagId,
                                MyFeedTags.TYPE to MyFeedTags.Type.EXCLUDED.value
                        )
                    }
                }

                for (tagId in oldExcludedTagIds) {
                    if (!newExcludedTagIds.contains(tagId)) {
                        MyFeedTags.delete(MyFeedTags.DeleteMyFeedTagCriteria(tagId, MyFeedTags.Type.EXCLUDED))
                    }
                }
            }

            launch(Dispatchers.Main) {
                activity?.finish()
            }
        }

        return true
    }

    class MyFeedTag(val id: Long, val name: String) {
        override fun toString(): String = name

        object QueryHelper : MyFeedTags.QueryHelper<MyFeedTag>(
                MyFeedTags.TAG_ID,
                MyFeedTags.TAG_NAME
        ) {
            override fun createRow(cursor: Cursor) = MyFeedTag(
                    id = cursor.getLong(0),
                    name = cursor.getString(1)
            )
        }
    }

    class Tag(val id: Long, val name: String) {
        override fun toString(): String = name

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

    class MyFeedSettingsViewModel : ViewModel() {
        var oldIncludedTagIds = LongArray(0)
        var newIncludedTagIds = MutableLiveData<LongArray>()
        val includedTags = MediatorLiveData<List<MyFeedTag>>()

        var oldExcludedTagIds = LongArray(0)
        var newExcludedTagIds = MutableLiveData<LongArray>()
        val excludedTags = MediatorLiveData<List<MyFeedTag>>()

        init {
            val liveTags = Tags.liveQuery(Tags.QueryAllTagsCriteria, Tag.QueryHelper)

            val liveIncludedMyFeedTags = MyFeedTags.liveQuery(MyFeedTags.QueryMyFeedTagsCriteria(MyFeedTags.Type.INCLUDED), MyFeedTag.QueryHelper)
            includedTags.addSource(liveIncludedMyFeedTags) {
                val includedTagIds = LongArray(it.size) { index -> it[index].id }
                oldIncludedTagIds = includedTagIds
                newIncludedTagIds.value = includedTagIds

                includedTags.removeSource(liveIncludedMyFeedTags)
            }
            includedTags.addSource(liveTags) { updateMyFeedTags(includedTags, it, newIncludedTagIds.value) }
            includedTags.addSource(newIncludedTagIds) { updateMyFeedTags(includedTags, liveTags.value, it) }

            val liveExcludedMyFeedTags = MyFeedTags.liveQuery(MyFeedTags.QueryMyFeedTagsCriteria(MyFeedTags.Type.EXCLUDED), MyFeedTag.QueryHelper)
            excludedTags.addSource(liveExcludedMyFeedTags) {
                val excludedTagIds = LongArray(it.size) { index -> it[index].id }
                oldExcludedTagIds = excludedTagIds
                newExcludedTagIds.value = excludedTagIds

                excludedTags.removeSource(liveExcludedMyFeedTags)
            }
            excludedTags.addSource(liveTags) { updateMyFeedTags(excludedTags, it, newExcludedTagIds.value) }
            excludedTags.addSource(newExcludedTagIds) { updateMyFeedTags(excludedTags, liveTags.value, it) }
        }

        private fun updateMyFeedTags(myFeedTags: MediatorLiveData<List<MyFeedTag>>, tags: List<Tag>?, selectedTagIds: LongArray?) {
            when {
                tags == null || selectedTagIds == null -> return
                selectedTagIds.isEmpty() -> myFeedTags.value = emptyList()
                else -> {
                    val newFeedTags = ArrayList<MyFeedTag>()
                    for (tag in tags) {
                        if (selectedTagIds.contains(tag.id)) {
                            newFeedTags.add(MyFeedTag(tag.id, tag.name))
                        }
                    }
                    myFeedTags.value = newFeedTags
                }
            }
        }
    }

}
