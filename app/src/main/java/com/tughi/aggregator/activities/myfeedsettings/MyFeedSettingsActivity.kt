package com.tughi.aggregator.activities.myfeedsettings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tughi.aggregator.AppActivity

class MyFeedSettingsActivity : AppActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, MyFeedSettingsActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragmentManager = supportFragmentManager
        var fragment = fragmentManager.findFragmentById(android.R.id.content)
        if (fragment == null) {
            fragment = MyFeedSettingsFragment()
            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit()
        }
    }

}
