package com.tughi.aggregator.activities.reader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.EntriesQueryCriteria
import kotlinx.coroutines.launch

class ReaderActivity : AppActivity(), ViewPager.OnPageChangeListener {

    companion object {
        private const val EXTRA_ENTRIES_QUERY_CRITERIA = "entries_query_criteria"
        private const val EXTRA_ENTRIES_POSITION = "entries_position"
    }

    val style by lazy {
        val styledAttributes = obtainStyledAttributes(intArrayOf(R.attr.colorAccent, android.R.attr.colorBackground))

        val accentColor = styledAttributes.getColor(0, Color.rgb(0xFF, 0x66, 0x00))

        @SuppressLint("ResourceType")
        val backgroundColor = styledAttributes.getColor(1, 0)

        styledAttributes.recycle()

        return@lazy Style(accentColor, backgroundColor)
    }

    private var entries: List<Entry> = emptyList()

    private lateinit var adapter: ReaderAdapter

    private lateinit var viewModel: ReaderViewModel

    private lateinit var actionBar: ActionBar

    private lateinit var pager: ViewPager

    private lateinit var resultData: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_back)
        }

        adapter = ReaderAdapter()

        val queryCriteria =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_ENTRIES_QUERY_CRITERIA, Entries.QueryCriteria::class.java)!!
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(EXTRA_ENTRIES_QUERY_CRITERIA)!! as Entries.QueryCriteria
            }

        val viewModelFactory = ReaderViewModel.Factory(queryCriteria)
        viewModel = ViewModelProvider(this, viewModelFactory).get(ReaderViewModel::class.java)

        viewModel.entries.observe(this) { entries ->
            this.entries = entries

            adapter.notifyDataSetChanged()

            val entriesPosition = resultData.getIntExtra(EXTRA_ENTRIES_POSITION, 0)
            if (pager.currentItem == entriesPosition) {
                onPageSelected(entriesPosition)
            } else {
                pager.setCurrentItem(entriesPosition, false)
            }
        }

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }

        return true
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

            contentScope.launch {
                Entries.update(Entries.UpdateEntryCriteria(entry.id), Entries.READ_TIME to System.currentTimeMillis())
            }
        }

        resultData.putExtra(EXTRA_ENTRIES_POSITION, position)
    }

    override fun onPageScrollStateChanged(state: Int) {
        // nothing to do here
    }

    @Suppress("DEPRECATION")
    private inner class ReaderAdapter : FragmentStatePagerAdapter(supportFragmentManager) {
        override fun getCount(): Int = entries.size

        override fun getItem(position: Int): Fragment {
            val entry = entries[position]
            return ReaderFragment().apply {
                arguments = Bundle().apply {
                    putLong(ReaderFragment.ARG_ENTRY_ID, entry.id)
                }
            }
        }
    }

    data class Entry(
        val id: Long
    ) {
        object QueryHelper : Entries.QueryHelper<Entry>(
            Entries.ID
        ) {
            override fun createRow(cursor: Cursor) = Entry(
                id = cursor.getLong(0)
            )
        }
    }

    class ReaderViewModel(queryCriteria: Entries.QueryCriteria) : ViewModel() {
        val entries: LiveData<List<Entry>> = MediatorLiveData<List<Entry>>().apply {
            val source = Entries.liveQuery(queryCriteria, Entry.QueryHelper)
            addSource(source) {
                value = it
                removeSource(source)
            }
        }

        class Factory(private val queryCriteria: Entries.QueryCriteria) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ReaderViewModel(queryCriteria) as T
                }
                throw UnsupportedOperationException()
            }
        }
    }

    data class Style(val accentColor: Int, val backgroundColor: Int) {
        val accentHexColor = asHexColor(accentColor)

        private fun asHexColor(color: Int) = "#%06x".format(color and 0xffffff)
    }

    data class ReadSessionInput(val entriesQueryCriteria: EntriesQueryCriteria, val position: Int)

    class ReadSession : ActivityResultContract<ReadSessionInput, Int>() {
        override fun createIntent(context: Context, input: ReadSessionInput): Intent =
            Intent(context, ReaderActivity::class.java)
                .putExtra(EXTRA_ENTRIES_QUERY_CRITERIA, input.entriesQueryCriteria)
                .putExtra(EXTRA_ENTRIES_POSITION, input.position)

        override fun parseResult(resultCode: Int, intent: Intent?): Int {
            if (resultCode == RESULT_OK) {
                return intent!!.getIntExtra(EXTRA_ENTRIES_POSITION, -1)
            }
            return -1
        }
    }

}
