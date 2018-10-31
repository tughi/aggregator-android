package com.tughi.aggregator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.tughi.aggregator.utilities.APP_THEME_DARK
import com.tughi.aggregator.utilities.APP_THEME_LIGHT

abstract class AppActivity : AppCompatActivity() {

    private var activityTheme: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        with(App.theme.value) {
            activityTheme = this

            setTheme(when {
                this == APP_THEME_LIGHT && this@AppActivity is MainActivity -> R.style.LightTheme_NoActionBar
                this == APP_THEME_LIGHT -> R.style.LightTheme
                this == APP_THEME_DARK && this@AppActivity is MainActivity -> R.style.DarkTheme_NoActionBar
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
