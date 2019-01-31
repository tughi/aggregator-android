package com.tughi.aggregator.activities.updatesettings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.updatemode.UpdateModeActivity
import com.tughi.aggregator.activities.updatemode.startUpdateModeActivity
import com.tughi.aggregator.activities.updatemode.toString
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.preferences.UpdateSettings
import com.tughi.aggregator.services.AutoUpdateScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UpdateSettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val REQUEST_UPDATE_MODE = 1
    }

    private lateinit var defaultUpdateModePrefence: Preference

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

        defaultUpdateModePrefence = findPreference(UpdateSettings.PREFERENCE_DEFAULT_UPDATE_MODE).apply {
            summary = UpdateSettings.defaultUpdateMode.toString(context)
            setOnPreferenceClickListener {
                startUpdateModeActivity(REQUEST_UPDATE_MODE, UpdateSettings.defaultUpdateMode, false)
                return@setOnPreferenceClickListener true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_UPDATE_MODE && resultCode == Activity.RESULT_OK) {
            val serializedUpdateMode = data?.getStringExtra(UpdateModeActivity.EXTRA_UPDATE_MODE)
                    ?: return
            UpdateMode.deserialize(serializedUpdateMode).also {
                UpdateSettings.defaultUpdateMode = it
                defaultUpdateModePrefence.summary = it.toString(defaultUpdateModePrefence.context)
            }
        }
    }

}
