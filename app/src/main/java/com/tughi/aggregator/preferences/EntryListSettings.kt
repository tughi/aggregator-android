package com.tughi.aggregator.preferences

import com.tughi.aggregator.App
import com.tughi.aggregator.data.Entries

object EntryListSettings {

    private const val PREFERENCE_ENTRIES_SORT_ORDER = "entries_sort_order"
    private const val PREFERENCE_SHOW_READ_ENTRIES = "show_read_entries"

    private val preferences = App.preferences

    var entriesSortOrder: Entries.SortOrder
        get() {
            val value = preferences.getString(PREFERENCE_ENTRIES_SORT_ORDER, null)
            if (value != null) {
                return Entries.SortOrder.deserialize(value)
            }
            return Entries.SortOrder.ByDateAscending
        }
        set(value) {
            preferences.edit()
                    .putString(PREFERENCE_ENTRIES_SORT_ORDER, value.serialize())
                    .apply()
        }

    var showReadEntries: Boolean
        get() {
            return preferences.getBoolean(PREFERENCE_SHOW_READ_ENTRIES, false)
        }
        set(value) {
            preferences.edit()
                    .putBoolean(PREFERENCE_SHOW_READ_ENTRIES, value)
                    .apply()
        }

}
