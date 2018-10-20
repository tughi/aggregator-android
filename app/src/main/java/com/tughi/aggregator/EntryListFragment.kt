package com.tughi.aggregator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.data.UiEntriesGetter
import com.tughi.aggregator.data.UiEntry
import com.tughi.aggregator.viewmodels.EntryListViewModel
import org.jetbrains.anko.displayMetrics
import kotlin.math.min

abstract class EntryListFragment : Fragment() {

    private val toolbarElevationMax by lazy { context!!.displayMetrics.density * 4 }

    private lateinit var toolbar: Toolbar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.entry_list_fragment, container, false)

        val viewModelFactory = EntryListViewModel.Factory(getUiEntriesGetter())
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(EntryListViewModel::class.java)

        val entriesRecyclerView = fragmentView.findViewById<RecyclerView>(R.id.entries)
        val progressBar = fragmentView.findViewById<ProgressBar>(R.id.progress)

        entriesRecyclerView.adapter = EntriesAdapter().also { adapter ->
            viewModel.entries.observe(this, Observer { entries ->
                adapter.submitList(entries)

                progressBar.visibility = View.GONE
            })
        }

        toolbar = fragmentView.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(when (this) {
            is MyFeedFragment -> R.drawable.action_menu
            else -> R.drawable.action_back
        })
        toolbar.setNavigationOnClickListener { onNavigationClick() }

        entriesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                toolbar.elevation = min(recyclerView.computeVerticalScrollOffset().toFloat(), toolbarElevationMax)
            }
        })

        return fragmentView
    }

    abstract fun getUiEntriesGetter(): UiEntriesGetter

    abstract fun onNavigationClick()

    protected fun setTitle(@StringRes title: Int) {
        toolbar.setTitle(title)
    }

    protected fun setTitle(title: String) {
        toolbar.title = title
    }

}

private class EntriesAdapter : PagedListAdapter<UiEntry, EntryViewHolder>(EntriesDiffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.entry_list_item, parent, false)
        return EntryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = getItem(position)

        if (entry != null) {
            holder.entry = entry

            holder.itemView.visibility = View.VISIBLE

            holder.feedTitle.text = entry.feedTitle
            holder.title.text = entry.title
            holder.favicon.setImageResource(R.drawable.favicon_placeholder)
            holder.time.text = entry.formattedTime.toString()
        } else {
            holder.itemView.visibility = View.INVISIBLE
        }
    }
}

private class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    val favicon: ImageView = itemView.findViewById(R.id.favicon)
    val title: TextView = itemView.findViewById(R.id.title)
    val feedTitle: TextView = itemView.findViewById(R.id.feed_title)
    val time: TextView = itemView.findViewById(R.id.time)

    lateinit var entry: UiEntry

    init {
        itemView.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        itemView.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.link)))

        // TODO: mark entry as read
    }
}

private object EntriesDiffUtil : DiffUtil.ItemCallback<UiEntry>() {

    override fun areItemsTheSame(oldItem: UiEntry, newItem: UiEntry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: UiEntry, newItem: UiEntry): Boolean {
        return oldItem == newItem
    }

}
