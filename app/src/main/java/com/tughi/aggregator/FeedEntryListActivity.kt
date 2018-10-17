package com.tughi.aggregator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class FeedEntryListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(supportFragmentManager) {
            var fragment = findFragmentById(android.R.id.content)
            if (fragment == null) {
                fragment = FeedEntryListFragment.newInstance(intent.getLongExtra(EXTRA_FEED_ID, 0))
                beginTransaction()
                        .replace(android.R.id.content, fragment)
                        .commit()
            }
        }
    }

    companion object {
        const val EXTRA_FEED_ID = "feed_id"
    }

}
