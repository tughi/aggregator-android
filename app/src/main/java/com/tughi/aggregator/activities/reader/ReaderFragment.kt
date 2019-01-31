package com.tughi.aggregator.activities.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.R

class ReaderFragment : Fragment() {

    companion object {
        internal const val ARG_ENTRY_ID = "entry_id"
        internal const val ARG_ENTRY_READ_TIME = "entry_read_time"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.reader_entry_fragment, container, false)

        arguments?.also { arguments ->
            val entryId = arguments.getLong(ARG_ENTRY_ID)
            val entryReadTime = arguments.getLong(ARG_ENTRY_READ_TIME)

            val viewModelFactory = ReaderFragmentViewModel.Factory(entryId, entryReadTime)
            val viewModel = ViewModelProviders.of(this, viewModelFactory).get(ReaderFragmentViewModel::class.java)

            val webView: WebView = fragmentView.findViewById(R.id.content)

            viewModel.entry.observe(this, Observer { entry ->
                webView.loadDataWithBaseURL(entry.link, entry.content, "text/html", null, null)
            })
        }

        return fragmentView
    }

}
