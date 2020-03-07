package com.tughi.aggregator.activities.feedsettings

import android.os.Bundle
import com.tughi.aggregator.AppActivity

class FeedSettingsActivity : AppActivity() {

    companion object {
        const val EXTRA_FEED_ID = "feed_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragmentManager = supportFragmentManager
        var fragment = fragmentManager.findFragmentById(android.R.id.content)
        if (fragment == null) {
            val intentExtras = intent.extras!!
            fragment = FeedSettingsFragment().apply {
                arguments = Bundle().apply {
                    putLong(FeedSettingsFragment.ARG_FEED_ID, intentExtras.getLong(EXTRA_FEED_ID))
                }
            }
            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit()
        }
    }

}
