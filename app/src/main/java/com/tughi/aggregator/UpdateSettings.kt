package com.tughi.aggregator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.tughi.aggregator.data.AdaptiveUpdateMode
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.services.AutoUpdateScheduler
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

    private lateinit var defaultUpdateModePrefence: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.update_settings, rootKey)

        val backgroundUpdatesPreference = findPreference(PREFERENCE_BACKGROUND_UPDATES)
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

        defaultUpdateModePrefence = findPreference(PREFERENCE_DEFAULT_UPDATE_MODE).apply {
            summary = UpdateSettings.defaultUpdateMode.toString(context)
            setOnPreferenceClickListener {
                startUpdateModeActivity(REQUEST_UPDATE_MODE, UpdateSettings.defaultUpdateMode, false)
                return@setOnPreferenceClickListener true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_UPDATE_MODE && resultCode == Activity.RESULT_OK) {
            val serializedUpdateMode = data?.getStringExtra(UpdateModeActivity.EXTRA_UPDATE_MODE) ?: return
            UpdateMode.deserialize(serializedUpdateMode).also {
                UpdateSettings.defaultUpdateMode = it
                defaultUpdateModePrefence.summary = it.toString(defaultUpdateModePrefence.context)
            }
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
            return AdaptiveUpdateMode
        }
        set(updateMode) {
            preferences.edit()
                    .putString(PREFERENCE_DEFAULT_UPDATE_MODE, updateMode.serialize())
                    .apply()

            GlobalScope.launch {
                AutoUpdateScheduler.scheduleFeedsWithDefaultUpdateMode()
            }
        }

}
