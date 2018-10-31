package com.tughi.aggregator

import android.os.Bundle

class FeedSettingsActivity : AppActivity() {

    companion object {
        const val EXTRA_FEED_ID = "feed_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.feed_settings_fragment)
    }

}
