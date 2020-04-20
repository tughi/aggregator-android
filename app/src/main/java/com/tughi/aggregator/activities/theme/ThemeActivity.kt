package com.tughi.aggregator.activities.theme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ThemeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeDialogFragment.show(supportFragmentManager)
    }
}
