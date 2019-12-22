package com.tughi.aggregator.preferences

import com.tughi.aggregator.App

object MyFeedSettings {

    private const val PREFERENCE_NOTIFICATION = "my_feed__notification"

    private val preferences = App.preferences

    var notification: Boolean
        get() {
            return preferences.getBoolean(PREFERENCE_NOTIFICATION, false)
        }
        set(value) {
            preferences.edit()
                    .putBoolean(PREFERENCE_NOTIFICATION, value)
                    .apply()
        }

}
