package com.tughi.aggregator.preferences

import com.tughi.aggregator.App
import com.tughi.aggregator.data.AdaptiveUpdateMode
import com.tughi.aggregator.data.UpdateMode
import com.tughi.aggregator.services.AutoUpdateScheduler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object UpdateSettings {

    const val PREFERENCE_BACKGROUND_UPDATES = "background_updates"
    const val PREFERENCE_DEFAULT_UPDATE_MODE = "default_update_mode"

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
