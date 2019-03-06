package com.tughi.aggregator.preferences

import com.tughi.aggregator.App
import com.tughi.aggregator.data.EntriesRepository

object EntryListSettings {

    private const val PREFERENCE_ENTRIES_SORT_ORDER = "entries_sort_order"

    private val preferences = App.preferences

    var entriesSortOrder: EntriesRepository.SortOrder
        get() {
            val value = preferences.getString(PREFERENCE_ENTRIES_SORT_ORDER, null)
            if (value != null) {
                return EntriesRepository.SortOrder.deserialize(value)
            }
            return EntriesRepository.SortOrder.ByDateAscending
        }
        set(value) {
            preferences.edit()
                    .putString(PREFERENCE_ENTRIES_SORT_ORDER, value.serialize())
                    .apply()
        }

}
