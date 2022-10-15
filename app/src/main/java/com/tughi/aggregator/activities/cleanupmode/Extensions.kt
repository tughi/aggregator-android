package com.tughi.aggregator.activities.cleanupmode

import android.content.Context
import com.tughi.aggregator.R
import com.tughi.aggregator.data.Age1MonthCleanupMode
import com.tughi.aggregator.data.Age1WeekCleanupMode
import com.tughi.aggregator.data.Age1YearCleanupMode
import com.tughi.aggregator.data.Age3DaysCleanupMode
import com.tughi.aggregator.data.Age3MonthsCleanupMode
import com.tughi.aggregator.data.Age3YearsCleanupMode
import com.tughi.aggregator.data.Age6MonthsCleanupMode
import com.tughi.aggregator.data.Age6YearsCleanupMode
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.DefaultCleanupMode
import com.tughi.aggregator.data.NeverCleanupMode
import com.tughi.aggregator.preferences.UpdateSettings

fun CleanupMode.toString(context: Context): String = when (this) {
    DefaultCleanupMode -> context.getString(R.string.cleanup_mode__default, UpdateSettings.defaultCleanupMode.toString(context))
    Age3DaysCleanupMode -> context.getString(R.string.cleanup_mode__age__3_days)
    Age1WeekCleanupMode -> context.getString(R.string.cleanup_mode__age__1_week)
    Age1MonthCleanupMode -> context.getString(R.string.cleanup_mode__age__1_month)
    Age3MonthsCleanupMode -> context.getString(R.string.cleanup_mode__age__3_months)
    Age6MonthsCleanupMode -> context.getString(R.string.cleanup_mode__age__6_months)
    Age1YearCleanupMode -> context.getString(R.string.cleanup_mode__age__1_year)
    Age3YearsCleanupMode -> context.getString(R.string.cleanup_mode__age__3_years)
    Age6YearsCleanupMode -> context.getString(R.string.cleanup_mode__age__6_years)
    NeverCleanupMode -> context.getString(R.string.cleanup_mode__never)
}
