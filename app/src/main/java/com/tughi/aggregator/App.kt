package com.tughi.aggregator

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        instance = this

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        style.value = Style(
                preferences.getEnum(PREF_STYLE_THEME, Style.Theme.DARK),
                preferences.getEnum(PREF_STYLE_ACCENT, Style.Accent.ORANGE),
                preferences.getEnum(PREF_STYLE_NAVIGATION_BAR, Style.NavigationBar.ACCENT)
        )

        Notifications.setupChannels(this)
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

        val accentColor: Int
            get() = ResourcesCompat.getColor(
                    instance.resources,
                    when (style.value!!.accent) {
                        Style.Accent.BLUE -> R.color.accent__blue
                        Style.Accent.GREEN -> R.color.accent__green
                        Style.Accent.ORANGE -> R.color.accent__orange
                        Style.Accent.PURPLE -> R.color.accent__purple
                        Style.Accent.RED -> R.color.accent__red
                    },
                    null
            )
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

        enum class NavigationBar(val default: Int) {
            ACCENT(R.style.BottomNavigationAccent),
            GRAY(R.style.BottomNavigationGray)
        }
    }

}
