package com.tughi.aggregator.activities.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.reader.ReaderActivity
import com.tughi.aggregator.data.EntriesRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class EntriesFragment : Fragment(), EntriesFragmentAdapterListener {

    private lateinit var viewModel: EntriesFragmentViewModel

    private lateinit var toolbar: Toolbar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.entry_list_fragment, container, false)

        val viewModelFactory = EntriesFragmentViewModel.Factory(initialEntriesQuery)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(EntriesFragmentViewModel::class.java)

        val entriesRecyclerView = fragmentView.findViewById<RecyclerView>(R.id.entries)
        val progressBar = fragmentView.findViewById<ProgressBar>(R.id.progress)

        val adapter = EntriesFragmentEntryAdapter(this)
        viewModel.entries.observe(this, Observer { entries ->
            adapter.submitList(entries)

            if (entries == null) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        })

        entriesRecyclerView.adapter = adapter
        ItemTouchHelper(SwipeItemTouchHelper()).attachToRecyclerView(entriesRecyclerView)

        toolbar = fragmentView.findViewById(R.id.toolbar)
        toolbar.setNavigationIcon(when (this) {
            is MyFeedFragment -> R.drawable.action_menu
            else -> R.drawable.action_back
        })
        toolbar.setNavigationOnClickListener { onNavigationClick() }

        toolbar.inflateMenu(R.menu.entry_list_fragment)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.show_read_entries -> {
                    viewModel.changeShowRead(!it.isChecked)
                }
                R.id.sort_by_date_asc -> {
                    viewModel.changeSortOrder(EntriesRepository.SortOrder.ByDateAscending)
                }
                R.id.sort_by_date_desc -> {
                    viewModel.changeSortOrder(EntriesRepository.SortOrder.ByDateDescending)
                }
                R.id.sort_by_title -> {
                    viewModel.changeSortOrder(EntriesRepository.SortOrder.ByTitle)
                }
                R.id.mark_all_read -> {
                    viewModel.queryCriteria.value?.let { queryCriteria ->
                        GlobalScope.launch {
                            EntriesRepository.markEntriesRead(queryCriteria)
                        }
                    }
                }
            }
            return@setOnMenuItemClickListener true
        }

        viewModel.queryCriteria.observe(this, Observer { entriesQuery ->
            toolbar.menu?.let {
                val sortMenuItemId = when (entriesQuery.sortOrder) {
                    EntriesRepository.SortOrder.ByDateAscending -> R.id.sort_by_date_asc
                    EntriesRepository.SortOrder.ByDateDescending -> R.id.sort_by_date_desc
                    EntriesRepository.SortOrder.ByTitle -> R.id.sort_by_title
                }
                it.findItem(sortMenuItemId).isChecked = true

                it.findItem(R.id.show_read_entries).isChecked = entriesQuery.sessionTime == 0L
            }
        })

        return fragmentView
    }

    internal abstract val initialEntriesQuery: EntriesRepository.QueryCriteria

    abstract fun onNavigationClick()

    protected fun setTitle(@StringRes title: Int) {
        toolbar.setTitle(title)
    }

    protected fun setTitle(title: String) {
        toolbar.title = title
    }

    override fun onEntryClicked(entry: EntriesFragmentViewModel.Entry, position: Int) {
        context?.run {
            startActivity(
                    Intent(this, ReaderActivity::class.java)
                            .putExtra(ReaderActivity.EXTRA_ENTRIES_QUERY_CRITERIA, viewModel.queryCriteria.value)
                            .putExtra(ReaderActivity.EXTRA_ENTRIES_POSITION, position)
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
                val entry = viewHolder.entry
                GlobalScope.launch {
                    if (entry.readTime == 0L || entry.pinnedTime != 0L) {
                        EntriesRepository.markEntryRead(entry.id)
                    } else {
                        EntriesRepository.markEntryPinned(entry.id)
                    }
                }
            }
        }

    }

}

