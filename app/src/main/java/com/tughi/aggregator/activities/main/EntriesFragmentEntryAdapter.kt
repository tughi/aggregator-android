package com.tughi.aggregator.activities.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R

internal class EntriesFragmentEntryAdapter(private val listener: EntriesFragmentAdapterListener) : RecyclerView.Adapter<EntriesFragmentViewHolder>() {

    var items: List<EntriesFragmentViewModel.Item> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = items.size

    override fun getItemId(position: Int) = items[position].id

    override fun getItemViewType(position: Int): Int = when (val item = items[position]) {
        is EntriesFragmentViewModel.Divider -> R.layout.entry_list_divider
        is EntriesFragmentViewModel.Entry -> if (item.unread) R.layout.entry_list_unread_entry else R.layout.entry_list_read_entry
        is EntriesFragmentViewModel.Header -> R.layout.entry_list_header
        is EntriesFragmentViewModel.Placeholder -> R.layout.entry_list_placeholder
        else -> throw IllegalStateException("Unsupported item type")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntriesFragmentViewHolder = when (viewType) {
        R.layout.entry_list_divider -> EntriesFragmentDividerViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
        R.layout.entry_list_header -> EntriesFragmentHeaderViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
        R.layout.entry_list_placeholder -> EntriesFragmentPlaceholderViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
        R.layout.entry_list_read_entry -> EntriesFragmentReadEntryViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false), listener)
        R.layout.entry_list_unread_entry -> EntriesFragmentUnreadEntryViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false), listener)
        else -> throw IllegalStateException("Unsupported view type")
    }

    override fun onBindViewHolder(holder: EntriesFragmentViewHolder, position: Int) = holder.onBind(items[position])

}
