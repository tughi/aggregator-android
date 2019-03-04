package com.tughi.aggregator.activities.main

import android.text.format.DateUtils
import com.tughi.aggregator.App

object MainTypeConverters {

    private const val DATE_FORMAT = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
    private const val TIME_FORMAT = DateUtils.FORMAT_SHOW_TIME

    private val context by lazy { App.instance.applicationContext }

//    @TypeConverter
    @JvmStatic
    fun formatDate(timestamp: Long): FormattedDate {
        return FormattedDate(DateUtils.formatDateTime(context, timestamp, DATE_FORMAT))
    }

//    @TypeConverter
    @JvmStatic
    fun formatTime(timestamp: Long): FormattedTime {
        return FormattedTime(DateUtils.formatDateTime(context, timestamp, TIME_FORMAT))
    }

//    @TypeConverter
    @JvmStatic
    fun convertEntriesFragmentEntryType(type: Int): EntriesFragmentEntryType {
        return EntriesFragmentEntryType.values()[type]
    }

}

data class FormattedDate(private val text: String) {
    override fun toString(): String {
        return text
    }
}

data class FormattedTime(private val text: String) {
    override fun toString(): String {
        return text
    }
}
