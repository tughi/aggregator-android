package com.tughi.aggregator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.tughi.aggregator.utilities.APP_THEME_LIGHT

abstract class AppActivity : AppCompatActivity() {

    private lateinit var activityTheme: String

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Application.theme.value.also { activityTheme = it!! }) {
            APP_THEME_LIGHT -> {
                setTheme(R.style.LightTheme)
            }
            else -> {
                // default theme
                setTheme(R.style.DarkTheme)
            }
        }

        super.onCreate(savedInstanceState)

        Application.theme.observe(this, Observer { theme ->
            if (theme != activityTheme) {
                recreate()
            }
        })
    }

}
