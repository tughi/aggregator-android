package com.tughi.aggregator.activities.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.reader.ReaderActivity
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.EntriesQueryCriteria
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class EntriesFragment : Fragment(), EntriesFragmentAdapterListener, Toolbar.OnMenuItemClickListener {

    companion object {
        private const val REQUEST_ENTRY_POSITION = 1
    }

    private lateinit var viewModel: EntriesFragmentViewModel

    private lateinit var toolbar: Toolbar

    private lateinit var entriesRecyclerView: RecyclerView
    private lateinit var entriesLayoutManager: LinearLayoutManager

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENTRY_POSITION && resultCode == Activity.RESULT_OK) {
            val entryPosition = data?.extras?.getInt(ReaderActivity.EXTRA_ENTRIES_POSITION, -1) ?: -1
            if (entryPosition != -1) {
                entriesLayoutManager.scrollToPositionWithOffset(entryPosition * 2 + 1, entriesRecyclerView.height / 3)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.entry_list_fragment, container, false)

        val viewModelFactory = EntriesFragmentViewModel.Factory(initialQueryCriteria)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(EntriesFragmentViewModel::class.java)

        entriesRecyclerView = fragmentView.findViewById(R.id.entries)
        entriesLayoutManager = entriesRecyclerView.layoutManager as LinearLayoutManager

        val progressBar = fragmentView.findViewById<ProgressBar>(R.id.progress)

        val adapter = EntriesFragmentEntryAdapter(this)
        viewModel.items.observe(this, Observer { items ->
            adapter.items = items

            if (items == null) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        })

        entriesRecyclerView.adapter = adapter
        entriesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val itemsRangeStart = viewModel.itemsRangeStart.value ?: 0
                val itemsRangeSize = viewModel.itemsRangeSize

                var newItemsRangeStart = itemsRangeStart
                if (dy > 0) {
                    val lastVisibleItemPosition = entriesLayoutManager.findLastVisibleItemPosition()
                    while (lastVisibleItemPosition - newItemsRangeStart > itemsRangeSize / 2) {
                        newItemsRangeStart += itemsRangeSize / 3
                    }
                } else {
                    val firstVisibleItemPosition = entriesLayoutManager.findFirstVisibleItemPosition()
                    while (newItemsRangeStart > 0 && firstVisibleItemPosition - newItemsRangeStart < itemsRangeSize / 2) {
                        newItemsRangeStart -= itemsRangeSize / 3
                    }
                }
                if (newItemsRangeStart != itemsRangeStart) {
                    viewModel.itemsRangeStart.value = newItemsRangeStart
                }
            }
        })
        ItemTouchHelper(SwipeItemTouchHelper()).attachToRecyclerView(entriesRecyclerView)

        toolbar = fragmentView.findViewById(R.id.toolbar)
        toolbar.setNavigationIcon(when (this) {
            is MyFeedFragment -> R.drawable.action_menu
            else -> R.drawable.action_back
        })
        toolbar.setNavigationOnClickListener { onNavigationClick() }

        toolbar.inflateMenu(R.menu.entry_list_fragment)
        toolbar.setOnMenuItemClickListener(this)

        viewModel.queryCriteria.observe(this, Observer { entriesQuery ->
            toolbar.menu?.let {
                val sortMenuItemId = when (entriesQuery.sortOrder) {
                    Entries.SortOrder.ByDateAscending -> R.id.sort_by_date_asc
                    Entries.SortOrder.ByDateDescending -> R.id.sort_by_date_desc
                    Entries.SortOrder.ByTitle -> R.id.sort_by_title
                }
                it.findItem(sortMenuItemId).isChecked = true

                it.findItem(R.id.show_read_entries).isChecked = entriesQuery.sessionTime == 0L
            }
        })

        return fragmentView
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.show_read_entries -> {
                viewModel.changeShowRead(!item.isChecked)
            }
            R.id.sort_by_date_asc -> {
                viewModel.changeSortOrder(Entries.SortOrder.ByDateAscending)
            }
            R.id.sort_by_date_desc -> {
                viewModel.changeSortOrder(Entries.SortOrder.ByDateDescending)
            }
            R.id.sort_by_title -> {
                viewModel.changeSortOrder(Entries.SortOrder.ByTitle)
            }
            R.id.mark_all_read -> {
                viewModel.queryCriteria.value?.let { queryCriteria ->
                    GlobalScope.launch {
                        Entries.markRead(queryCriteria)
                    }
                }
            }
        }

        return true
    }

    internal abstract val initialQueryCriteria: EntriesQueryCriteria

    abstract fun onNavigationClick()

    protected fun setTitle(@StringRes title: Int) {
        toolbar.setTitle(title)
    }

    protected fun setTitle(title: String) {
        toolbar.title = title
    }

    override fun onEntryClicked(entry: EntriesFragmentViewModel.Entry, position: Int) {
        context?.run {
            startActivityForResult(
                    Intent(this, ReaderActivity::class.java)
                            .putExtra(ReaderActivity.EXTRA_ENTRIES_QUERY_CRITERIA, viewModel.queryCriteria.value)
                            .putExtra(ReaderActivity.EXTRA_ENTRIES_POSITION, position),
                    REQUEST_ENTRY_POSITION
            )
        }
    }

    private class SwipeItemTouchHelper : ItemTouchHelper.Callback() {

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                when (viewHolder) {
                    is EntriesFragmentEntryViewHolder -> makeMovementFlags(0, ItemTouchHelper.START or ItemTouchHelper.END)
                    else -> 0
                }


        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            if (viewHolder is EntriesFragmentEntryViewHolder) {
                val entry = viewHolder.item as EntriesFragmentViewModel.Entry
                GlobalScope.launch {
                    if (entry.readTime == 0L || entry.pinnedTime != 0L) {
                        Entries.markRead(entry.id)
                    } else {
                        Entries.markPinned(entry.id)
                    }
                }
            }
        }

    }

}

