package com.tughi.aggregator.activities.reader

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.webkit.WebViewClientCompat
import com.tughi.aggregator.App
import com.tughi.aggregator.BuildConfig
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.entrytags.EntryTagsActivity
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.EntryTags
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.utilities.Html
import com.tughi.aggregator.utilities.Language
import com.tughi.aggregator.utilities.openURL
import com.tughi.aggregator.utilities.shareLink
import kotlinx.coroutines.launch
import java.nio.charset.Charset


class ReaderFragment : Fragment(), MenuProvider {

    companion object {
        internal const val ARG_ENTRY_ID = "entry_id"

        private val ENTRY_LINK_URL = Uri.Builder()
            .scheme("https")
            .authority(BuildConfig.APPLICATION_ID)
            .path("entry-link")
            .build()

        private val entryTemplate by lazy {
            App.instance.resources
                .openRawResource(R.raw.entry)
                .readBytes()
                .toString(Charset.defaultCharset())
        }
    }

    private lateinit var markDoneMenuItem: MenuItem
    private lateinit var markPinnedMenuItem: MenuItem
    private lateinit var addStarMenuItem: MenuItem
    private lateinit var removeStarMenuItem: MenuItem

    private var loadedEntry: Entry? = null
    private var loadedEntryLink: String? = null
    private var loadedEntryHtml: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().addMenuProvider(this, this, Lifecycle.State.RESUMED)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.reader_entry_fragment, container, false)

        arguments?.also { arguments ->
            val entryId = arguments.getLong(ARG_ENTRY_ID)

            val viewModelFactory = ReaderViewModel.Factory(entryId)
            val viewModel = ViewModelProvider(this, viewModelFactory).get(ReaderViewModel::class.java)

            val webView: WebView = fragmentView.findViewById(R.id.content)
            webView.webViewClient = CustomWebViewClient()

            webView.settings.apply {
                javaScriptEnabled = false // TODO: enable once content is sanitized
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            webView.scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY

            val style = (activity as ReaderActivity).style

            webView.setBackgroundColor(style.backgroundColor)

            viewModel.entry.observe(viewLifecycleOwner, Observer { entry ->
                loadedEntry = entry ?: return@Observer

                val entryLink = entry.link
                val entryTitle = entry.title
                val entryFeedTitle = entry.feedTitle
                val entryFeedLanguage = entry.feedLanguage
                val entryPublished = entry.publishTime
                val entryContent = entry.content
                val entryAuthor = entry.author

                val entryTitleHtml = if (entryTitle != null) {
                    if (entryLink != null) {
                        """<a href="$ENTRY_LINK_URL">${Html.encode(entryTitle)}</a>"""
                    } else {
                        Html.encode(entryTitle)
                    }
                } else {
                    ""
                }

                // TODO: run this in a coroutine
                val entryHtml = entryTemplate
                    .replace("#ff6600", style.accentHexColor)
                    .replace("{{ reader.theme }}", App.style.value?.theme?.name?.lowercase() ?: "")
                    .replace("{{ layout_direction }}", if (Language.isRightToLeft(entryFeedLanguage)) "rtl" else "ltr")
                    .replace("{{ entry.source }}", if (entryAuthor != null) "$entryFeedTitle — $entryAuthor" else entryFeedTitle)
                    .replace("{{ entry.date }}", DateUtils.formatDateTime(activity, entryPublished, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR))
                    .replace("{{ entry.title }}", entryTitleHtml)
                    .replace("{{ entry.content }}", entryContent ?: "")

                if (entryHtml != loadedEntryHtml) {
                    loadedEntryLink = entryLink
                    loadedEntryHtml = entryHtml
                    webView.loadDataWithBaseURL(entryLink, entryHtml, "text/html", null, null)
                }

                activity?.invalidateOptionsMenu()
            })
        }

        return fragmentView
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.reader_activity, menu)

        markDoneMenuItem = menu.findItem(R.id.mark_done)
        markPinnedMenuItem = menu.findItem(R.id.mark_pinned)
        addStarMenuItem = menu.findItem(R.id.add_star)
        removeStarMenuItem = menu.findItem(R.id.remove_star)
    }

    override fun onPrepareMenu(menu: Menu) {
        val read = loadedEntry?.run { readTime != 0L && pinnedTime == 0L } ?: false

        markDoneMenuItem.isVisible = !read
        markPinnedMenuItem.isVisible = read

        val starred = loadedEntry?.run { starredTime != 0L } ?: false

        addStarMenuItem.isVisible = !starred
        removeStarMenuItem.isVisible = starred
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mark_done -> {
                loadedEntry?.let {
                    contentScope.launch {
                        EntryTags.delete(EntryTags.DeleteEntryTagCriteria(it.id, Tags.PINNED))
                    }
                }
            }
            R.id.mark_pinned -> {
                loadedEntry?.let {
                    contentScope.launch {
                        EntryTags.insert(
                            EntryTags.ENTRY_ID to it.id,
                            EntryTags.TAG_ID to Tags.PINNED,
                            EntryTags.TAG_TIME to System.currentTimeMillis()
                        )
                    }
                }
            }
            R.id.add_star -> {
                loadedEntry?.let {
                    contentScope.launch {
                        EntryTags.insert(
                            EntryTags.ENTRY_ID to it.id,
                            EntryTags.TAG_ID to Tags.STARRED,
                            EntryTags.TAG_TIME to System.currentTimeMillis()
                        )
                    }
                }
            }
            R.id.remove_star -> {
                loadedEntry?.let {
                    contentScope.launch {
                        EntryTags.delete(EntryTags.DeleteEntryTagCriteria(it.id, Tags.STARRED))
                    }
                }
            }
            R.id.share -> {
                loadedEntryLink?.also {
                    activity?.shareLink(it)
                }
            }
            R.id.tag -> {
                context?.let { context ->
                    loadedEntry?.let { entry ->
                        EntryTagsActivity.start(context, entry.id)
                    }
                }
            }
            else -> {
                return false
            }
        }

        return true
    }

    data class Entry(
        val id: Long,
        val feedId: Long,
        val title: String?,
        val link: String?,
        val content: String?,
        val author: String?,
        val publishTime: Long,
        val feedTitle: String,
        val feedLanguage: String?,
        val readTime: Long,
        val pinnedTime: Long,
        val starredTime: Long
    ) {
        object QueryHelper : Entries.QueryHelper<Entry>(
            Entries.ID,
            Entries.FEED_ID,
            Entries.TITLE,
            Entries.LINK,
            Entries.CONTENT,
            Entries.AUTHOR,
            Entries.PUBLISH_TIME,
            Entries.FEED_TITLE,
            Entries.FEED_LANGUAGE,
            Entries.READ_TIME,
            Entries.PINNED_TIME,
            Entries.STARRED_TIME
        ) {
            override fun createRow(cursor: Cursor) = Entry(
                id = cursor.getLong(0),
                feedId = cursor.getLong(1),
                title = cursor.getString(2),
                link = cursor.getString(3),
                content = cursor.getString(4),
                author = cursor.getString(5),
                publishTime = cursor.getLong(6),
                feedTitle = cursor.getString(7),
                feedLanguage = cursor.getString(8),
                readTime = cursor.getLong(9),
                pinnedTime = cursor.getLong(10),
                starredTime = cursor.getLong(11)
            )
        }
    }

    internal class ReaderViewModel(entryId: Long) : ViewModel() {
        val entry = Entries.liveQueryOne(Entries.QueryRowCriteria(entryId), Entry.QueryHelper)

        class Factory(private val entryId: Long) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ReaderViewModel(entryId) as T
                }
                throw UnsupportedOperationException()
            }
        }
    }

    inner class CustomWebViewClient : WebViewClientCompat() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return shouldOverrideUrlLoading(request.url)
        }

        private fun shouldOverrideUrlLoading(requestUrl: Uri): Boolean {
            if (requestUrl == ENTRY_LINK_URL) {
                loadedEntryLink?.let {
                    requireContext().openURL(it)
                }
            } else {
                requireContext().openURL(requestUrl)
            }
            return true
        }
    }

}
