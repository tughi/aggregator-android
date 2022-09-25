package com.tughi.aggregator.preferences

import com.tughi.aggregator.App

object User {

    private const val PREFERENCE_LAST_SEEN = "user__last_seen"

    var lastSeen: Long
        get() {
            return App.preferences.getLong(PREFERENCE_LAST_SEEN, 0)
        }
        set(value) {
            App.preferences.edit()
                .putLong(PREFERENCE_LAST_SEEN, value)
                .apply()
        }

}
