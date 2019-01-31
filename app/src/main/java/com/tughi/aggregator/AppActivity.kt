package com.tughi.aggregator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.tughi.aggregator.activities.main.MainActivity
import com.tughi.aggregator.utilities.APP_THEME_DARK
import com.tughi.aggregator.utilities.APP_THEME_LIGHT

abstract class AppActivity : AppCompatActivity() {

    private var activityTheme: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        App.theme.value.let { theme ->
            activityTheme = theme

            setTheme(when {
                theme == APP_THEME_LIGHT && this is MainActivity -> R.style.LightTheme_NoActionBar
                theme == APP_THEME_LIGHT -> R.style.LightTheme
                theme == APP_THEME_DARK && this is MainActivity -> R.style.DarkTheme_NoActionBar
                else -> R.style.DarkTheme
            })
        }

        super.onCreate(savedInstanceState)

        App.theme.observe(this, Observer { theme ->
            if (theme != activityTheme) {
                recreate()
            }
        })
    }

}
