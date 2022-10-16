package com.tughi.aggregator.activities.myfeedsettings

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.tagspicker.TagsPickerActivity
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.MyFeedTags
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.preferences.MyFeedSettings
import com.tughi.aggregator.widgets.DropDownButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFeedSettingsFragment : Fragment() {

    private val viewModel by lazy { ViewModelProvider(this)[MyFeedSettingsViewModel::class.java] }

    private lateinit var notificationSwitch: SwitchCompat
    private lateinit var includedTags: DropDownButton
    private lateinit var excludedTags: DropDownButton

    private val requestIncludedTags = registerForActivityResult(TagsPickerActivity.PickTags()) { selectedTagIds ->
        if (selectedTagIds != null) {
            viewModel.newIncludedTagIds.value = selectedTagIds
        }
    }

    private val requestExcludedTags = registerForActivityResult(TagsPickerActivity.PickTags()) { selectedTagIds ->
        if (selectedTagIds != null) {
            viewModel.newExcludedTagIds.value = selectedTagIds
        }
    }

    private val requestPostNotificationsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        viewModel.newNotificationValue.value = isGranted

        if (!isGranted) {
            Toast.makeText(requireContext(), R.string.my_feed_settings__notification__denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.my_feed_settings_fragment, container, false)

        notificationSwitch = fragmentView.findViewById(R.id.notification)
        notificationSwitch.isChecked = viewModel.newNotificationValue.value == true
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.newNotificationValue.value = true
                } else {
                    notificationSwitch.isChecked = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPostNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                viewModel.newNotificationValue.value = false
            }
        }
        viewModel.newNotificationValue.observe(viewLifecycleOwner) {
            notificationSwitch.isChecked = it
        }

        includedTags = fragmentView.findViewById(R.id.included_tags)
        includedTags.setOnClickListener {
            requestIncludedTags.launch(
                TagsPickerActivity.PickTagsRequest(viewModel.newIncludedTagIds.value, title = R.string.my_feed_settings__included_tags)
            )
        }
        viewModel.includedTags.observe(viewLifecycleOwner) { tags ->
            if (tags.isNullOrEmpty()) {
                includedTags.setText(R.string.my_feed_settings__included_tags__none)
            } else {
                includedTags.setText(tags.joinToString())
            }
        }

        excludedTags = fragmentView.findViewById(R.id.excluded_tags)
        excludedTags.setOnClickListener {
            requestExcludedTags.launch(
                TagsPickerActivity.PickTagsRequest(viewModel.newExcludedTagIds.value, title = R.string.my_feed_settings__excluded_tags)
            )
        }
        viewModel.excludedTags.observe(viewLifecycleOwner) { tags ->
            if (tags.isNullOrEmpty()) {
                excludedTags.setText(R.string.my_feed_settings__excluded_tags__none)
            } else {
                excludedTags.setText(tags.joinToString())
            }
        }

        fragmentView.findViewById<Button>(R.id.save).setOnClickListener {
            onSave()
        }

        return fragmentView
    }

    private fun onSave() {
        val newNotificationValue = viewModel.newNotificationValue.value == true
        if (newNotificationValue != viewModel.oldNotificationValue) {
            MyFeedSettings.notification = newNotificationValue
        }

        val oldIncludedTagIds = viewModel.oldIncludedTagIds
        val newIncludedTagIds = viewModel.newIncludedTagIds.value ?: LongArray(0)
        val oldExcludedTagIds = viewModel.oldExcludedTagIds
        val newExcludedTagIds = viewModel.newExcludedTagIds.value ?: LongArray(0)

        contentScope.launch {
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
        val oldNotificationValue = MyFeedSettings.notification
        var newNotificationValue = MutableLiveData(MyFeedSettings.notification)

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
