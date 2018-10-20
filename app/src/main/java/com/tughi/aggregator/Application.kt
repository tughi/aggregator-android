package com.tughi.aggregator

import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.utilities.APP_THEME_DARK
import com.tughi.aggregator.utilities.APP_THEME_LIGHT
import com.tughi.aggregator.utilities.PREF_APP_THEME

class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()

        instance = this

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        Application.theme.value = when (preferences.getString(PREF_APP_THEME, null)) {
            APP_THEME_LIGHT -> APP_THEME_LIGHT
            else -> APP_THEME_DARK
        }

    }

    companion object {
        lateinit var instance: Application
            private set

        lateinit var preferences: SharedPreferences
            private set

        val theme = MutableLiveData<String>()
    }

}
