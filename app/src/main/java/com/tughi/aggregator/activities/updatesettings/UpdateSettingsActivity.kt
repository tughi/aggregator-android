package com.tughi.aggregator.activities.updatesettings

import android.os.Bundle
import com.tughi.aggregator.AppActivity

class UpdateSettingsActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentById(android.R.id.content) == null) {
            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, UpdateSettingsFragment())
                    .commit()
        }
    }

}

