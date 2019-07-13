package com.tughi.aggregator

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.utilities.PREF_STYLE_ACCENT
import com.tughi.aggregator.utilities.PREF_STYLE_NAVIGATION_BAR
import com.tughi.aggregator.utilities.PREF_STYLE_THEME

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        instance = this

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        App.style.value = Style(
                preferences.getEnum(PREF_STYLE_THEME, Style.Theme.DARK),
                preferences.getEnum(PREF_STYLE_ACCENT, Style.Accent.ORANGE),
                preferences.getEnum(PREF_STYLE_NAVIGATION_BAR, Style.NavigationBar.ACCENT)
        )
    }

    private inline fun <reified T : Enum<T>> SharedPreferences.getEnum(key: String, default: T): T {
        val name = getString(key, default.name)
        if (name != null) {
            try {
                return enumValueOf(name)
            } catch (exception: Exception) {
                // ignored
            }
        }
        return default
    }

    companion object {
        lateinit var instance: App
            private set

        lateinit var preferences: SharedPreferences
            private set

        val style = object : MutableLiveData<Style>() {
            private var oldValue: Style? = null

            override fun setValue(value: Style) {
                super.setValue(value)

                val oldValue = oldValue
                if (oldValue != value) {
                    if (oldValue != null) {
                        preferences.edit()
                                .putString(PREF_STYLE_THEME, value.theme.name)
                                .putString(PREF_STYLE_ACCENT, value.accent.name)
                                .putString(PREF_STYLE_NAVIGATION_BAR, value.navigationBar.name)
                                .apply()
                    }
                    this.oldValue = value
                }
            }
        }
    }

    data class Style(val theme: Theme, val accent: Accent, val navigationBar: NavigationBar) {
        enum class Theme(val default: Int, val withoutActionBar: Int) {
            DARK(R.style.DarkTheme, R.style.DarkTheme_NoActionBar),
            LIGHT(R.style.LightTheme, R.style.LightTheme_NoActionBar)
        }

        enum class Accent(val default: Int) {
            BLUE(R.style.AccentBlue),
            GREEN(R.style.AccentGreen),
            ORANGE(R.style.AccentOrange),
            PURPLE(R.style.AccentPurple),
            RED(R.style.AccentRed)
        }

        enum class NavigationBar(val dark: Int, val light: Int) {
            ACCENT(R.style.BottomNavigationAccent, R.style.BottomNavigationAccent),
            GRAY(R.style.BottomNavigationDark, R.style.BottomNavigationLight)
        }
    }

}
