package com.tughi.aggregator.preferences

import com.tughi.aggregator.App

object MyFeedSettings {

    private const val PREFERENCE_NOTIFICATION = "my_feed__notification"

    var notification: Boolean
        get() {
            return App.preferences.getBoolean(PREFERENCE_NOTIFICATION, true)
        }
        set(value) {
            App.preferences.edit()
                .putBoolean(PREFERENCE_NOTIFICATION, value)
                .apply()
        }

}
