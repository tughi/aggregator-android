package com.tughi.aggregator

import android.app.Dialog
import android.app.job.JobScheduler
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceFragmentCompat
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
        /* TODO
        defaultUpdateModePreference.setOnPreferenceClickListener {
            UpdateModeDialogFragment.show(fragmentManager!!, false)
            return@setOnPreferenceClickListener true
        }
        */
    }

}

object UpdateSettings {

    private val preferences = App.preferences

    val backgroundUpdates: Boolean
        get() = preferences.getBoolean(PREFERENCE_BACKGROUND_UPDATES, true)

}

class UpdateModeDialogFragment : DialogFragment() {

    companion object {
        const val ARG_WITH_DEFAULT = "feed_id"

        fun show(fragmentManager: FragmentManager, withDefault: Boolean = true) {
            UpdateModeDialogFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putBoolean(ARG_WITH_DEFAULT, withDefault)
                        }
                    }
                    .show(fragmentManager, "update-mode-dialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = arguments!!
        val withDefault = arguments.getBoolean(ARG_WITH_DEFAULT)
        return AlertDialog.Builder(context!!)
                .setTitle(R.string.feed_settings__update_mode)
                // TODO: .setSingleChoiceItems(...)
                .create()
    }

}
