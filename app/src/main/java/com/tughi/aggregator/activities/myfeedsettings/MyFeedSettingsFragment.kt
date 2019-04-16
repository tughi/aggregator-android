package com.tughi.aggregator.activities.myfeedsettings

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.R
import com.tughi.aggregator.data.MyFeedTags
import com.tughi.aggregator.widgets.makeClickable

class MyFeedSettingsFragment : Fragment() {

    private lateinit var includedTags: EditText
    private lateinit var excludedTags: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.my_feed_settings_fragment, container, false)

        val viewModel = ViewModelProviders.of(this).get(MyFeedSettingsViewModel::class.java)

        includedTags = fragmentView.findViewById(R.id.included_tags)
        includedTags.makeClickable { }
        viewModel.includedTags.observe(this, Observer { tags ->
            if (tags.isNullOrEmpty()) {
                includedTags.setText(R.string.my_feed_settings__included_tags__none)
            } else {
                includedTags.setText(tags.joinToString())
            }
        })

        excludedTags = fragmentView.findViewById(R.id.excluded_tags)
        excludedTags.makeClickable { }
        viewModel.excludedTags.observe(this, Observer { tags ->
            if (tags.isNullOrEmpty()) {
                excludedTags.setText(R.string.my_feed_settings__excluded_tags__none)
            } else {
                excludedTags.setText(tags.joinToString())
            }
        })

        return fragmentView
    }

    class Tag(val name: String) {
        override fun toString(): String = name

        object QueryHelper : MyFeedTags.QueryHelper<Tag>(
                MyFeedTags.TAG_NAME
        ) {
            override fun createRow(cursor: Cursor) = Tag(
                    name = cursor.getString(0)
            )
        }
    }

    class MyFeedSettingsViewModel : ViewModel() {
        val includedTags = MyFeedTags.liveQuery(MyFeedTags.QueryMyFeedTagsCriteria(MyFeedTags.Type.INCLUDED), Tag.QueryHelper)
        val excludedTags = MyFeedTags.liveQuery(MyFeedTags.QueryMyFeedTagsCriteria(MyFeedTags.Type.EXCLUDED), Tag.QueryHelper)
    }

}
