package com.tughi.aggregator.preferences

import androidx.core.app.NotificationManagerCompat
import com.tughi.aggregator.App

object MyFeedSettings {

    private const val PREFERENCE_NOTIFICATION = "my_feed__notification"

    var notification: Boolean
        get() {
            if (NotificationManagerCompat.from(App.instance).areNotificationsEnabled()) {
                return App.preferences.getBoolean(PREFERENCE_NOTIFICATION, false)
            }
            return false
        }
        set(value) {
            App.preferences.edit()
                .putBoolean(PREFERENCE_NOTIFICATION, value)
                .apply()
        }

}
