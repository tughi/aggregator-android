package com.tughi.aggregator.activities.reader

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.tughi.aggregator.App
import com.tughi.aggregator.R
import com.tughi.aggregator.utilities.Language
import java.nio.charset.Charset


class ReaderFragment : Fragment() {

    companion object {
        internal const val ARG_ENTRY_ID = "entry_id"
        internal const val ARG_ENTRY_READ_TIME = "entry_read_time"

        private val entryTemplate by lazy {
            App.instance.resources
                    .openRawResource(R.raw.entry)
                    .readBytes()
                    .toString(Charset.defaultCharset())
        }
    }

    private var loadedEntryHtml: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.reader_entry_fragment, container, false)

        arguments?.also { arguments ->
            val entryId = arguments.getLong(ARG_ENTRY_ID)
            val entryReadTime = arguments.getLong(ARG_ENTRY_READ_TIME)

            val viewModelFactory = ReaderFragmentViewModel.Factory(entryId, entryReadTime)
            val viewModel = ViewModelProviders.of(this, viewModelFactory).get(ReaderFragmentViewModel::class.java)

            val webView: WebView = fragmentView.findViewById(R.id.content)

            webView.settings.apply {
                javaScriptEnabled = false // TODO: enable one content is sanitized
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            webView.scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY;

            val attributes = context!!.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
            webView.setBackgroundColor(attributes.getColor(0, 0))
            attributes.recycle()

            viewModel.entry.observe(this, Observer { entry ->
                val entryLink = entry.link
                val entryTitle = entry.title
                val entryFeedTitle = entry.feedTitle
                val entryFeedLanguage = entry.feedLanguage
                val entryPublished = entry.publishTime
                val entryContent = entry.content
                val entryAuthor = entry.author

                val entryHtml = entryTemplate
                        .replace("{{ reader.theme }}", App.theme.value?.toLowerCase() ?: "")
                        .replace("{{ layout_direction }}", if (Language.isRightToLeft(entryFeedLanguage)) "rtl" else "ltr")
                        .replace("{{ entry.feed_name }}", entryFeedTitle)
                        .replace("{{ entry.link }}", entryLink ?: "#")
                        .replace("{{ entry.date }}", DateUtils.formatDateTime(activity, entryPublished, 0))
                        .replace("{{ entry.title }}", entryTitle ?: "")
                        .replace("{{ entry.author }}", entryAuthor ?: "")
                        .replace("{{ entry.content }}", entryContent ?: "")

                if (entryHtml != loadedEntryHtml) {
                    loadedEntryHtml = entryHtml
                    webView.loadDataWithBaseURL(entryLink, entryHtml, "text/html", null, null)
                }
            })
        }

        return fragmentView
    }

}
