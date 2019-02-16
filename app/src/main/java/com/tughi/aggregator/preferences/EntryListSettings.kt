package com.tughi.aggregator.preferences

import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.App
import com.tughi.aggregator.data.EntriesSortOrder
import com.tughi.aggregator.data.EntriesSortOrderByDateAsc

object EntryListSettings {

    private const val PREFERENCE_ENTRIES_SORT_ORDER = "entries_sort_order"

    private val preferences = App.preferences

    val entriesSortOrder = object : MutableLiveData<EntriesSortOrder>() {
        override fun setValue(value: EntriesSortOrder?) {
            super.setValue(value)

            preferences.edit()
                    .putString(PREFERENCE_ENTRIES_SORT_ORDER, value!!.serialize())
                    .apply()
        }
    }.also {
        val value = preferences.getString(PREFERENCE_ENTRIES_SORT_ORDER, null)
        it.value = if (value != null) EntriesSortOrder.deserialize(value) else EntriesSortOrderByDateAsc
    }

}
