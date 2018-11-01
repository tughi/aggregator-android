package com.tughi.aggregator

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.viewmodels.FeedSettingsViewModel

class FeedSettingsActivity : AppActivity() {

    companion object {
        const val EXTRA_FEED_ID = "feed_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragmentManager = supportFragmentManager
        var fragment = fragmentManager.findFragmentById(android.R.id.content)
        if (fragment == null) {
            val intentExtras = intent.extras!!
            val args = Bundle()
            args.putLong(FeedSettingsFragment.ARG_FEED_ID, intentExtras.getLong(EXTRA_FEED_ID))
            fragment = Fragment.instantiate(this, FeedSettingsFragment::class.java.name, args)
            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit()
        }
    }

}


class FeedSettingsFragment : Fragment() {

    companion object {
        const val ARG_FEED_ID = "feed_id"
    }

    lateinit var viewModel: FeedSettingsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.feed_settings_fragment, container, false)

        val urlEditText = fragmentView.findViewById<EditText>(R.id.url)
        val titleEditText = fragmentView.findViewById<EditText>(R.id.title)
        val updateModeTextView = fragmentView.findViewById<TextView>(R.id.update_mode)

        updateModeTextView.keyListener = null
        updateModeTextView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
        updateModeTextView.setOnClickListener {
            Log.d(javaClass.name, "Clicked")
        }


        val feedId = arguments!!.getLong(ARG_FEED_ID)
        viewModel = ViewModelProviders.of(this, FeedSettingsViewModel.Factory(feedId))
                .get(FeedSettingsViewModel::class.java)

        viewModel.feed.observe(this, Observer { feed ->
            if (feed != null) {
                urlEditText.setText(feed.url)
                titleEditText.setText(feed.customTitle ?: feed.title)
                updateModeTextView.text = feed.updateMode
            }
        })

        return fragmentView
    }

}
