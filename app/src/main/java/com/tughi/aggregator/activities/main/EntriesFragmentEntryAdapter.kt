package com.tughi.aggregator.activities.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.tughi.aggregator.R

internal class EntriesFragmentEntryAdapter(private val listener: EntriesFragmentAdapterListener) : ListAdapter<EntriesFragmentViewModel.Entry, EntriesFragmentViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = getItem(position).type.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntriesFragmentViewHolder = when (EntriesFragmentEntryType.values()[viewType]) {
        EntriesFragmentEntryType.DIVIDER -> EntriesFragmentDividerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.entry_list_divider, parent, false))
        EntriesFragmentEntryType.HEADER -> EntriesFragmentHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.entry_list_header, parent, false))
        EntriesFragmentEntryType.READ -> EntriesFragmentReadEntryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.entry_list_read_item, parent, false), listener)
        EntriesFragmentEntryType.UNREAD -> EntriesFragmentUnreadEntryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.entry_list_unread_item, parent, false), listener)
    }

    override fun onBindViewHolder(holder: EntriesFragmentViewHolder, position: Int) = holder.onBind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<EntriesFragmentViewModel.Entry>() {

        override fun areItemsTheSame(oldItem: EntriesFragmentViewModel.Entry, newItem: EntriesFragmentViewModel.Entry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EntriesFragmentViewModel.Entry, newItem: EntriesFragmentViewModel.Entry): Boolean {
            return oldItem == newItem
        }

    }

}
