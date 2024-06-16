package com.tughi.aggregator.activities.main

internal interface EntriesFragmentAdapterListener {

    fun onEntryClicked(entry: EntriesFragmentViewModel.Entry, position: Int)

    fun onEntrySelectorClicked(entry: EntriesFragmentViewModel.Entry, position: Int)

}
