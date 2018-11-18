package com.tughi.aggregator

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceFragmentCompat

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

const val PREFERENCE_DEFAULT_UPDATE_MODE = "default_update_mode"

class UpdateSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.update_settings, rootKey)

        val defaultUpdateModePreference = findPreference(PREFERENCE_DEFAULT_UPDATE_MODE)
        /* TODO
        defaultUpdateModePreference.setOnPreferenceClickListener {
            UpdateModeDialogFragment.show(fragmentManager!!, false)
            return@setOnPreferenceClickListener true
        }
        */
    }

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
