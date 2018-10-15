package com.tughi.aggregator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.data.UiEntry
import com.tughi.aggregator.viewmodels.EntryListViewModel

class MyFeedFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.my_feed_fragment, container, false)

        val viewModel = ViewModelProviders.of(this).get(EntryListViewModel::class.java)

        val entriesRecyclerView = view.findViewById<RecyclerView>(R.id.entries)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress)

        entriesRecyclerView.adapter = EntriesAdapter().also { adapter ->
            viewModel.entries.observe(this, Observer { entries ->
                adapter.submitList(entries)

                progressBar.visibility = View.GONE
            })
        }

        return view
    }

    companion object {
        fun newInstance(): MyFeedFragment {
            return MyFeedFragment()
        }
    }

}

private class EntriesAdapter : PagedListAdapter<UiEntry, EntryViewHolder>(EntryDiffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.entry_list_item, parent, false)
        return EntryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = getItem(position)

        if (entry != null) {
            holder.itemView.visibility = View.VISIBLE

            holder.title.text = entry.title
            holder.favicon.setImageResource(R.drawable.favicon_placeholder)
        } else {
            holder.itemView.visibility = View.INVISIBLE
        }
    }
}

private class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val favicon: ImageView = itemView.findViewById(R.id.favicon)
    val title: TextView = itemView.findViewById(R.id.title)
}

private object EntryDiffUtil : DiffUtil.ItemCallback<UiEntry>() {

    override fun areItemsTheSame(oldItem: UiEntry, newItem: UiEntry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: UiEntry, newItem: UiEntry): Boolean {
        return oldItem == newItem
    }

}
