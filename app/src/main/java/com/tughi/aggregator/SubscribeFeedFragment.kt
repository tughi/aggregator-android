package com.tughi.aggregator

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Feed
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class SubscribeFeedFragment : Fragment() {

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_URL = "url"
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
        updateModeTextView.setText(R.string.update_mode__default)

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
                val title = arguments!!.getString(ARG_TITLE)!!
                val customTitle = titleTextView.text.toString()
                doAsync {
                    val feedId = Database.get(context!!).feedDao().addFeed(Feed(
                            url = urlTextView.text.toString(),
                            title = title,
                            customTitle = if (customTitle != title) customTitle else null
                    ))

                    // TODO: update new feed
                    Log.d(javaClass.name, "Feed $feedId added...")

                    uiThread {
                        activity?.finish()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
