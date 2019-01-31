package com.tughi.aggregator.activities.updatemode

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.tughi.aggregator.R
import com.tughi.aggregator.UpdateSettings
import com.tughi.aggregator.data.AdaptiveUpdateMode
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.DisabledUpdateMode
import com.tughi.aggregator.data.Every15MinutesUpdateMode
import com.tughi.aggregator.data.Every2HoursUpdateMode
import com.tughi.aggregator.data.Every30MinutesUpdateMode
import com.tughi.aggregator.data.Every3HoursUpdateMode
import com.tughi.aggregator.data.Every45MinutesUpdateMode
import com.tughi.aggregator.data.Every4HoursUpdateMode
import com.tughi.aggregator.data.Every6HoursUpdateMode
import com.tughi.aggregator.data.Every8HoursUpdateMode
import com.tughi.aggregator.data.EveryHourUpdateMode
import com.tughi.aggregator.data.OnAppLaunchUpdateMode
import com.tughi.aggregator.data.UpdateMode

fun UpdateMode.toString(context: Context): String = when (this) {
    AdaptiveUpdateMode -> context.getString(R.string.update_mode__adaptive)
    DefaultUpdateMode -> context.getString(R.string.update_mode__default, UpdateSettings.defaultUpdateMode.toString(context))
    DisabledUpdateMode -> context.getString(R.string.update_mode__disabled)
    Every15MinutesUpdateMode -> context.getString(R.string.update_mode__every_15_minutes)
    Every30MinutesUpdateMode -> context.getString(R.string.update_mode__every_30_minutes)
    Every45MinutesUpdateMode -> context.getString(R.string.update_mode__every_45_minutes)
    EveryHourUpdateMode -> context.getString(R.string.update_mode__every_hour)
    Every2HoursUpdateMode -> context.getString(R.string.update_mode__every_2_hours)
    Every3HoursUpdateMode -> context.getString(R.string.update_mode__every_3_hours)
    Every4HoursUpdateMode -> context.getString(R.string.update_mode__every_4_hours)
    Every6HoursUpdateMode -> context.getString(R.string.update_mode__every_6_hours)
    Every8HoursUpdateMode -> context.getString(R.string.update_mode__every_8_hours)
    OnAppLaunchUpdateMode -> context.getString(R.string.update_mode__on_app_launch)
}

fun Fragment.startUpdateModeActivity(requestCode: Int, currentUpdateMode: UpdateMode, showDefault: Boolean = true) {
    val context = context ?: return
    val intent = Intent(context, UpdateModeActivity::class.java)
            .putExtra(UpdateModeActivity.EXTRA_UPDATE_MODE, currentUpdateMode.serialize())
            .putExtra(UpdateModeActivity.EXTRA_SHOW_DEFAULT, showDefault)
    startActivityForResult(intent, requestCode)
}

