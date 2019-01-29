package com.tughi.aggregator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.tughi.aggregator.data.EntriesQuery
import com.tughi.aggregator.data.ReaderEntry

class ReaderActivity : AppActivity(), ViewPager.OnPageChangeListener {

    companion object {
        const val EXTRA_ENTRIES_QUERY = "entries_query"
        const val EXTRA_ENTRIES_POSITION = "entries_position"
    }

    private var entries: Array<ReaderEntry> = emptyArray()

    private lateinit var adapter: ReaderAdapter

    private lateinit var viewModel: ReaderViewModel

    private lateinit var actionBar: ActionBar

    private lateinit var pager: ViewPager

    private lateinit var resultData: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ReaderAdapter()

        val entriesQuery = intent.getSerializableExtra(EXTRA_ENTRIES_QUERY) as EntriesQuery

        val viewModelFactory = ReaderViewModel.Factory(entriesQuery)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ReaderViewModel::class.java)

        viewModel.entries.observe(this, Observer { entries ->
            this.entries = entries

            adapter.notifyDataSetChanged()

            val entriesPosition = resultData.getIntExtra(EXTRA_ENTRIES_POSITION, 0)
            if (pager.currentItem == entriesPosition) {
                onPageSelected(entriesPosition)
            } else {
                pager.setCurrentItem(entriesPosition, false)
            }
        })

        setContentView(R.layout.reader_activity)

        actionBar = supportActionBar!!

        pager = findViewById(R.id.pager)
        pager.addOnPageChangeListener(this)
        pager.pageMargin = resources.getDimensionPixelSize(R.dimen.reader_pager_margin)
        pager.setPageMarginDrawable(R.drawable.reader_pager_margin)
        pager.adapter = adapter

        resultData = Intent().putExtra(EXTRA_ENTRIES_POSITION, intent.getIntExtra(EXTRA_ENTRIES_POSITION, 0))
        setResult(RESULT_OK, resultData)

        if (savedInstanceState != null) {
            resultData.putExtra(EXTRA_ENTRIES_POSITION, savedInstanceState.getInt(EXTRA_ENTRIES_POSITION))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_ENTRIES_POSITION, resultData.getIntExtra(EXTRA_ENTRIES_POSITION, 0))

        super.onSaveInstanceState(outState)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        // nothing to do here
    }

    override fun onPageSelected(position: Int) {
        if (entries.size > position) {
            val entry = entries[position]

            // update title
            actionBar.setDisplayShowTitleEnabled(false)
            actionBar.title = (position + 1).toString() + " / " + entries.size
            actionBar.setDisplayShowTitleEnabled(true)

            if (entry.readTime == 0L) {
                // TODO: ReaderEntryFragment.SetEntryFlagReadTask(this).execute(entry.id, System.currentTimeMillis())
            }
        }

        resultData.putExtra(EXTRA_ENTRIES_POSITION, position)
    }

    override fun onPageScrollStateChanged(state: Int) {
        // nothing to do here
    }

    private inner class ReaderAdapter : FragmentStatePagerAdapter(supportFragmentManager) {

        override fun getCount(): Int = entries.size

        override fun getItem(position: Int): Fragment {
            val entry = entries[position]
            val arguments = Bundle().apply {
                putLong(ReaderEntryFragment.ARG_ENTRY_ID, entry.id)
                putLong(ReaderEntryFragment.ARG_ENTRY_READ_TIME, entry.readTime)
            }
            return Fragment.instantiate(this@ReaderActivity, ReaderEntryFragment::class.java.name, arguments)
        }

    }

}

class ReaderViewModel(entriesQuery: EntriesQuery) : ViewModel() {

    val entries: LiveData<Array<ReaderEntry>> = AppDatabase.instance.entryDao().getReaderEntries(entriesQuery)

    class Factory(private val entriesQuery: EntriesQuery) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderViewModel(entriesQuery) as T
            }
            throw UnsupportedOperationException()
        }

    }

}

class ReaderViewPager(context: Context, attrs: AttributeSet) : ViewPager(context, attrs) {

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val pageMargin = pageMargin
        super.onSizeChanged(w - pageMargin, h, oldw - pageMargin, oldh)
    }

}

class ReaderEntryFragment : Fragment() {

    companion object {
        internal const val ARG_ENTRY_ID = "entry_id"
        internal const val ARG_ENTRY_READ_TIME = "entry_read_time"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.reader_entry_fragment, container, false)

        arguments?.also { arguments ->
            val entryId = arguments.getLong(ARG_ENTRY_ID)
            val entryReadTime = arguments.getLong(ARG_ENTRY_READ_TIME)

            val viewModelFactory = ReaderEntryViewModel.Factory(entryId, entryReadTime)
            val viewModel = ViewModelProviders.of(this, viewModelFactory).get(ReaderEntryViewModel::class.java)

            val webView: WebView = fragmentView.findViewById(R.id.content)

            viewModel.entry.observe(this, Observer { entry ->
                webView.loadDataWithBaseURL(entry.link, entry.content, "text/html", null, null)
            })
        }

        return fragmentView
    }

}

internal class ReaderEntryViewModel(entryId: Long, entryReadTime: Long) : ViewModel() {

    val entry = AppDatabase.instance.entryDao().getReaderEntryContent(entryId)

    class Factory(private val entryId: Long, private val entryReadTime: Long) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderEntryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderEntryViewModel(entryId, entryReadTime) as T
            }
            throw UnsupportedOperationException()
        }

    }

}
