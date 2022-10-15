package com.tughi.aggregator.activities.updatesettings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.cleanupmode.CleanupModeActivity
import com.tughi.aggregator.activities.cleanupmode.toString
import com.tughi.aggregator.activities.updatemode.UpdateModeActivity
import com.tughi.aggregator.activities.updatemode.toString
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.preferences.UpdateSettings
import com.tughi.aggregator.services.AutoUpdateScheduler
import kotlinx.coroutines.launch

class UpdateSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var defaultCleanupModePreference: Preference
    private lateinit var defaultUpdateModePreference: Preference

    private val requestCleanupMode = registerForActivityResult(CleanupModeActivity.PickCleanupMode()) { cleanupMode ->
        if (cleanupMode != null) {
            UpdateSettings.defaultCleanupMode = cleanupMode
            defaultCleanupModePreference.summary = cleanupMode.toString(defaultCleanupModePreference.context)
        }
    }

    private val requestUpdateMode = registerForActivityResult(UpdateModeActivity.PickUpdateMode()) { updateMode ->
        if (updateMode != null) {
            UpdateSettings.defaultUpdateMode = updateMode
            defaultUpdateModePreference.summary = updateMode.toString(defaultUpdateModePreference.context)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.update_settings, rootKey)

        val backgroundUpdatesPreference = findPreference<Preference>(UpdateSettings.PREFERENCE_BACKGROUND_UPDATES)!!
        backgroundUpdatesPreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                contentScope.launch {
                    AutoUpdateScheduler.schedule()
                }
            } else {
                AutoUpdateScheduler.cancel()
            }
            return@setOnPreferenceChangeListener true
        }

        defaultCleanupModePreference = findPreference<Preference>(UpdateSettings.PREFERENCE_DEFAULT_CLEANUP_MODE)!!.apply {
            summary = UpdateSettings.defaultCleanupMode.toString(context)
            setOnPreferenceClickListener {
                requestCleanupMode.launch(CleanupModeActivity.PickCleanupModeRequest(UpdateSettings.defaultCleanupMode, false))
                return@setOnPreferenceClickListener true
            }
        }

        defaultUpdateModePreference = findPreference<Preference>(UpdateSettings.PREFERENCE_DEFAULT_UPDATE_MODE)!!.apply {
            summary = UpdateSettings.defaultUpdateMode.toString(context)
            setOnPreferenceClickListener {
                requestUpdateMode.launch(UpdateModeActivity.PickUpdateModeRequest(UpdateSettings.defaultUpdateMode, false))
                return@setOnPreferenceClickListener true
            }
        }
    }

}
