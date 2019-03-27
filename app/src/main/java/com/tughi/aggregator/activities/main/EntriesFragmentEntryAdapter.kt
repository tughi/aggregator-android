package com.tughi.aggregator.activities.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R

internal class EntriesFragmentEntryAdapter(private val listener: EntriesFragmentAdapterListener) : RecyclerView.Adapter<EntriesFragmentViewHolder>() {

    var entries: List<EntriesFragmentViewModel.Entry> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemCount() = entries.size

    override fun getItemId(position: Int) = entries[position].id

    override fun getItemViewType(position: Int) = entries[position].type.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntriesFragmentViewHolder = when (EntriesFragmentEntryType.values()[viewType]) {
        EntriesFragmentEntryType.DIVIDER -> EntriesFragmentDividerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.entry_list_divider, parent, false))
        EntriesFragmentEntryType.HEADER -> EntriesFragmentHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.entry_list_header, parent, false))
        EntriesFragmentEntryType.READ -> EntriesFragmentReadEntryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.entry_list_read_item, parent, false), listener)
        EntriesFragmentEntryType.UNREAD -> EntriesFragmentUnreadEntryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.entry_list_unread_item, parent, false), listener)
    }

    override fun onBindViewHolder(holder: EntriesFragmentViewHolder, position: Int) = holder.onBind(entries[position])

}
