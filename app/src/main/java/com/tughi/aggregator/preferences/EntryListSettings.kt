package com.tughi.aggregator.preferences

import com.tughi.aggregator.App
import com.tughi.aggregator.data.EntriesSortOrder
import com.tughi.aggregator.data.EntriesSortOrderByDateAsc

object EntryListSettings {

    private const val PREFERENCE_ENTRIES_SORT_ORDER = "entries_sort_order"

    private val preferences = App.preferences

    var entriesSortOrder: EntriesSortOrder
        get() {
            val value = preferences.getString(PREFERENCE_ENTRIES_SORT_ORDER, null)
            if (value != null) {
                return EntriesSortOrder.deserialize(value)
            }
            return EntriesSortOrderByDateAsc
        }
        set(sortOrder) {
            preferences.edit()
                    .putString(PREFERENCE_ENTRIES_SORT_ORDER, sortOrder.serialize())
                    .apply()
        }

}
