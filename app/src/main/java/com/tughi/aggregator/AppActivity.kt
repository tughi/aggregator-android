package com.tughi.aggregator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.tughi.aggregator.activities.main.MainActivity

abstract class AppActivity : AppCompatActivity() {

    private var currentStyle: App.Style? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        App.style.value?.let { style ->
            currentStyle = style

            setTheme(when {
                this is MainActivity -> style.theme.withoutActionBar
                else -> style.theme.default
            })
            theme.apply {
                applyStyle(style.accent.default, true)

                when (style.theme) {
                    App.Style.Theme.LIGHT -> applyStyle(style.navigationBar.light, true)
                    else -> applyStyle(style.navigationBar.dark, true)
                }
            }
        }

        super.onCreate(savedInstanceState)

        App.style.observe(this, Observer { style ->
            if (style != currentStyle) {
                recreate()
            }
        })
    }

}
