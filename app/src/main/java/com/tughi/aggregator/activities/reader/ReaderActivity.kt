package com.tughi.aggregator.activities.reader

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.data.Entries
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ReaderActivity : AppActivity(), ViewPager.OnPageChangeListener {

    companion object {
        const val EXTRA_ENTRIES_QUERY_CRITERIA = "entries_query_criteria"
        const val EXTRA_ENTRIES_POSITION = "entries_position"
    }

    private var entries: List<ReaderActivityViewModel.Entry> = emptyList()

    private lateinit var adapter: ReaderAdapter

    private lateinit var viewModel: ReaderActivityViewModel

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

        val queryCriteria = intent.getSerializableExtra(EXTRA_ENTRIES_QUERY_CRITERIA) as Entries.QueryCriteria

        val viewModelFactory = ReaderActivityViewModel.Factory(queryCriteria)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ReaderActivityViewModel::class.java)

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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
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

            if (entry.readTime == 0L && entry.pinnedTime == 0L) {
                GlobalScope.launch {
                    Entries.markRead(entry.id)
                }
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
                putLong(ReaderFragment.ARG_ENTRY_ID, entry.id)
                putLong(ReaderFragment.ARG_ENTRY_READ_TIME, entry.readTime)
            }
            return Fragment.instantiate(this@ReaderActivity, ReaderFragment::class.java.name, arguments)
        }

    }

}
