package com.tughi.aggregator.activities.updatesettings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.cleanupmode.CleanupModeActivity
import com.tughi.aggregator.activities.cleanupmode.startCleanupModeActivity
import com.tughi.aggregator.activities.cleanupmode.toString
import com.tughi.aggregator.activities.updatemode.UpdateModeActivity
import com.tughi.aggregator.activities.updatemode.startUpdateModeActivity
import com.tughi.aggregator.activities.updatemode.toString
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.preferences.UpdateSettings
import com.tughi.aggregator.services.AutoUpdateScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UpdateSettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val REQUEST_CLEANUP_MODE = 1
        private const val REQUEST_UPDATE_MODE = 2
    }

    private lateinit var defaultCleanupModePreference: Preference
    private lateinit var defaultUpdateModePreference: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.update_settings, rootKey)

        val backgroundUpdatesPreference = findPreference(UpdateSettings.PREFERENCE_BACKGROUND_UPDATES)
        backgroundUpdatesPreference.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue == true) {
                GlobalScope.launch(Dispatchers.IO) {
                    AutoUpdateScheduler.schedule()
                }
            } else {
                AutoUpdateScheduler.cancel()
            }
            return@setOnPreferenceChangeListener true
        }

        defaultCleanupModePreference = findPreference(UpdateSettings.PREFERENCE_DEFAULT_CLEANUP_MODE).apply {
            summary = UpdateSettings.defaultCleanupMode.toString(context)
            setOnPreferenceClickListener {
                startCleanupModeActivity(REQUEST_CLEANUP_MODE, UpdateSettings.defaultCleanupMode, false)
                return@setOnPreferenceClickListener true
            }
        }

        defaultUpdateModePreference = findPreference(UpdateSettings.PREFERENCE_DEFAULT_UPDATE_MODE).apply {
            summary = UpdateSettings.defaultUpdateMode.toString(context)
            setOnPreferenceClickListener {
                startUpdateModeActivity(REQUEST_UPDATE_MODE, UpdateSettings.defaultUpdateMode, false)
                return@setOnPreferenceClickListener true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CLEANUP_MODE -> {
                    val serializedCleanupMode = data?.getStringExtra(CleanupModeActivity.EXTRA_CLEANUP_MODE) ?: return
                    CleanupMode.deserialize(serializedCleanupMode).also {
                        UpdateSettings.defaultCleanupMode = it
                        defaultCleanupModePreference.summary = it.toString(defaultCleanupModePreference.context)
                    }
                }
                REQUEST_UPDATE_MODE -> {
                    val serializedUpdateMode = data?.getStringExtra(UpdateModeActivity.EXTRA_UPDATE_MODE) ?: return
                    UpdateMode.deserialize(serializedUpdateMode).also {
                        UpdateSettings.defaultUpdateMode = it
                        defaultUpdateModePreference.summary = it.toString(defaultUpdateModePreference.context)
                    }
                }
            }
        }

    }

}
