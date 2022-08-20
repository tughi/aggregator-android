package com.tughi.aggregator.activities.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.reader.ReaderActivity
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.EntriesQueryCriteria
import com.tughi.aggregator.data.EntryTags
import com.tughi.aggregator.data.TagEntriesQueryCriteria
import com.tughi.aggregator.data.Tags
import com.tughi.aggregator.contentScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        viewModel = ViewModelProvider(this, viewModelFactory).get(EntriesFragmentViewModel::class.java)

        val progressBar = fragmentView.findViewById<ProgressBar>(R.id.progress)

        val emptyView = fragmentView.findViewById<View>(R.id.empty)
        val emptyMessageTextView = emptyView.findViewById<TextView>(R.id.message)
        val showReadEntriesButton = emptyView.findViewById<Button>(R.id.show_read_entries)

        showReadEntriesButton.setOnClickListener {
            viewModel.changeShowRead(true)
        }

        entriesRecyclerView = fragmentView.findViewById(R.id.entries)
        entriesLayoutManager = entriesRecyclerView.layoutManager as LinearLayoutManager

        val adapter = EntriesFragmentEntryAdapter(this)
        entriesRecyclerView.adapter = adapter

        viewModel.items.observe(viewLifecycleOwner, Observer { items ->
            adapter.items = items

            if (items == null) {
                progressBar.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
                if (items.isEmpty()) {
                    val entriesQueryCriteria = viewModel.entriesQueryCriteria.value!!
                    if (entriesQueryCriteria.showRead) {
                        if (entriesQueryCriteria is TagEntriesQueryCriteria) {
                            emptyMessageTextView.setText(R.string.entry_list__no_tagged_entries__all)
                        } else {
                            emptyMessageTextView.setText(R.string.entry_list__no_feed_entries__all)
                        }
                        showReadEntriesButton.visibility = View.GONE
                    } else {
                        if (entriesQueryCriteria is TagEntriesQueryCriteria) {
                            emptyMessageTextView.setText(R.string.entry_list__no_tagged_entries__unread)
                        } else {
                            emptyMessageTextView.setText(R.string.entry_list__no_feed_entries__unread)
                        }
                        showReadEntriesButton.visibility = View.VISIBLE
                    }

                    emptyView.visibility = View.VISIBLE
                } else {
                    emptyView.visibility = View.GONE
                }
            }
        })

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
        toolbar.setNavigationIcon(
            when (this) {
                is MyFeedFragment -> R.drawable.action_menu
                else -> R.drawable.action_back
            }
        )
        toolbar.setNavigationOnClickListener { onNavigationClick() }

        toolbar.inflateMenu(R.menu.entry_list_fragment)
        toolbar.setOnMenuItemClickListener(this)

        viewModel.entriesQueryCriteria.observe(viewLifecycleOwner, Observer { entriesQueryCriteria ->
            toolbar.menu?.let {
                val sortMenuItemId = when (entriesQueryCriteria.sortOrder) {
                    Entries.SortOrder.ByDateAscending -> R.id.sort_by_date_asc
                    Entries.SortOrder.ByDateDescending -> R.id.sort_by_date_desc
                    Entries.SortOrder.ByTitle -> R.id.sort_by_title
                }
                it.findItem(sortMenuItemId).isChecked = true

                it.findItem(R.id.show_read_entries).isChecked = entriesQueryCriteria.showRead
            }
        })

        val unreadEntriesTextView: TextView = fragmentView.findViewById(R.id.unread_entries)

        viewModel.unreadEntriesCount.observe(viewLifecycleOwner, Observer { unreadEntriesCount ->
            unreadEntriesTextView.text = if (unreadEntriesCount > 0) "%d".format(unreadEntriesCount) else ""
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
                viewModel.entriesQueryCriteria.value?.let { queryCriteria ->
                    contentScope.launch {
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
                    .putExtra(ReaderActivity.EXTRA_ENTRIES_QUERY_CRITERIA, viewModel.entriesQueryCriteria.value)
                    .putExtra(ReaderActivity.EXTRA_ENTRIES_POSITION, position),
                REQUEST_ENTRY_POSITION
            )
        }
    }

    private class SwipeItemTouchHelper : ItemTouchHelper.Callback() {

        private val scope = CoroutineScope(Dispatchers.Main)

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
                contentScope.launch {
                    if (entry.readTime == 0L || entry.pinnedTime != 0L) {
                        Database.transaction {
                            val updated = Entries.update(Entries.UpdateEntryCriteria(entry.id), Entries.READ_TIME to System.currentTimeMillis())
                            val deleted = EntryTags.delete(EntryTags.DeleteEntryTagCriteria(entry.id, Tags.PINNED))

                            Log.i(javaClass.name, "entry updated: $updated and removed pinned tags: $deleted")
                        }
                    } else {
                        EntryTags.insert(
                            EntryTags.ENTRY_ID to entry.id,
                            EntryTags.TAG_ID to Tags.PINNED,
                            EntryTags.TAG_TIME to System.currentTimeMillis()
                        )
                    }
                }
            }
        }

    }

}

