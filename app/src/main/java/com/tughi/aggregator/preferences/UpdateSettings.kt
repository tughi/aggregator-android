package com.tughi.aggregator.preferences

import com.tughi.aggregator.App
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.AdaptiveUpdateMode
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.services.AutoUpdateScheduler
import kotlinx.coroutines.launch

object UpdateSettings {

    const val PREFERENCE_BACKGROUND_UPDATES = "background_updates"
    const val PREFERENCE_DEFAULT_CLEANUP_MODE = "default_cleanup_mode"
    const val PREFERENCE_DEFAULT_UPDATE_MODE = "default_update_mode"

    val backgroundUpdates: Boolean
        get() = App.preferences.getBoolean(PREFERENCE_BACKGROUND_UPDATES, true)

    var defaultCleanupMode: CleanupMode
        get() {
            val value = App.preferences.getString(PREFERENCE_DEFAULT_CLEANUP_MODE, null)
            return CleanupMode.deserialize(value)
        }
        set(cleanupMode) {
            App.preferences.edit()
                .putString(PREFERENCE_DEFAULT_CLEANUP_MODE, cleanupMode.serialize())
                .apply()
        }

    var defaultUpdateMode: UpdateMode
        get() {
            val value = App.preferences.getString(PREFERENCE_DEFAULT_UPDATE_MODE, null)
            if (value != null) {
                return UpdateMode.deserialize(value)
            }
            return AdaptiveUpdateMode
        }
        set(updateMode) {
            App.preferences.edit()
                .putString(PREFERENCE_DEFAULT_UPDATE_MODE, updateMode.serialize())
                .apply()

            contentScope.launch {
                AutoUpdateScheduler.scheduleFeedsWithDefaultUpdateMode()
            }
        }

}
