package com.tughi.aggregator.activities.feedsettings

import android.os.Bundle
import androidx.fragment.app.Fragment
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
            val args = Bundle()
            args.putLong(FeedSettingsFragment.ARG_FEED_ID, intentExtras.getLong(EXTRA_FEED_ID))
            fragment = Fragment.instantiate(this, FeedSettingsFragment::class.java.name, args)
            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit()
        }
    }

}
