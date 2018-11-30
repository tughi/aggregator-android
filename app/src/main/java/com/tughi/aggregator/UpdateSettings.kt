package com.tughi.aggregator

import android.app.Activity
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.tughi.aggregator.data.AutoUpdateMode
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.services.FeedUpdaterScheduler
import com.tughi.aggregator.services.FeedsUpdaterService
import com.tughi.aggregator.utilities.JOB_SERVICE_FEEDS_UPDATER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UpdateSettingsActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentById(android.R.id.content) == null) {
            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, UpdateSettingsFragment())
                    .commit()
        }
    }

}

const val PREFERENCE_BACKGROUND_UPDATES = "background_updates"
const val PREFERENCE_DEFAULT_UPDATE_MODE = "default_update_mode"

class UpdateSettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val REQUEST_UPDATE_MODE = 1
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.update_settings, rootKey)

        val backgroundUpdatesPreference = findPreference(PREFERENCE_BACKGROUND_UPDATES)
        backgroundUpdatesPreference.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue == true) {
                GlobalScope.launch(Dispatchers.Main) {
                    FeedsUpdaterService.schedule()
                }
            } else {
                val jobScheduler = preference.context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                jobScheduler.cancel(JOB_SERVICE_FEEDS_UPDATER)
            }
            return@setOnPreferenceChangeListener true
        }

        val defaultUpdateModePreference = findPreference(PREFERENCE_DEFAULT_UPDATE_MODE)
        defaultUpdateModePreference.setOnPreferenceClickListener {
            startUpdateModeActivity(REQUEST_UPDATE_MODE, UpdateSettings.defaultUpdateMode, false)
            return@setOnPreferenceClickListener true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_UPDATE_MODE && resultCode == Activity.RESULT_OK) {
            val updateMode = data?.getStringExtra(UpdateModeActivity.EXTRA_UPDATE_MODE) ?: return
            UpdateSettings.defaultUpdateMode = UpdateMode.deserialize(updateMode)
        }
    }

}

object UpdateSettings {

    private val preferences = App.preferences

    val backgroundUpdates: Boolean
        get() = preferences.getBoolean(PREFERENCE_BACKGROUND_UPDATES, true)

    var defaultUpdateMode: UpdateMode
        get() {
            val value = preferences.getString(PREFERENCE_DEFAULT_UPDATE_MODE, null)
            if (value != null) {
                return UpdateMode.deserialize(value)
            }
            return AutoUpdateMode
        }
        set(updateMode) {
            preferences.edit()
                    .putString(PREFERENCE_DEFAULT_UPDATE_MODE, updateMode.serialize())
                    .apply()

            GlobalScope.launch {
                FeedUpdaterScheduler.scheduleFeedsWithDefaultUpdateMode()
            }
        }

}
