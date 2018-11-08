package com.tughi.aggregator

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.tughi.aggregator.data.Feed
import com.tughi.aggregator.services.FaviconUpdaterService
import com.tughi.aggregator.services.FeedUpdater
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class SubscribeFeedFragment : Fragment() {

    companion object {
        const val ARG_URL = "url"
        const val ARG_TITLE = "title"
        const val ARG_LINK = "link"
    }

    private lateinit var urlTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var updateModeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.subscribe_feed_fragment, container, false)
        val arguments = arguments!!

        urlTextView = fragmentView.findViewById(R.id.url)
        urlTextView.text = arguments.getString(ARG_URL)

        titleTextView = fragmentView.findViewById(R.id.title)
        titleTextView.text = arguments.getString(ARG_TITLE)

        updateModeTextView = fragmentView.findViewById(R.id.update_mode)
        updateModeTextView.keyListener = null
        updateModeTextView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
        updateModeTextView.setText(R.string.subscribe_feed__update_mode__default)

        return fragmentView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater?.inflate(R.menu.subscribe_feed_fragment, menu)
    }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(R.string.title_add_feed)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.add -> {
                val arguments = arguments!!
                val title = arguments.getString(ARG_TITLE)!!
                val customTitle = titleTextView.text.toString()
                val link = arguments.getString(ARG_LINK)!!
                doAsync {
                    val feedId = AppDatabase.instance.feedDao().insertFeed(Feed(
                            url = urlTextView.text.toString(),
                            title = title,
                            customTitle = if (customTitle != title) customTitle else null,
                            link = link
                    ))

                    uiThread {
                        FeedUpdater().update(feedId)

                        FaviconUpdaterService.start(feedId)

                        activity?.finish()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
