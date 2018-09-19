package com.tughi.aggregator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val navigation by lazy { findViewById<BottomNavigationView>(R.id.navigation) }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val fragment = when (item.itemId) {
            R.id.navigation_my_feeds -> MyFeedFragment.newInstance()
            R.id.navigation_tags -> TagsFragment.newInstance()
            R.id.navigation_feeds -> FeedListFragment.newInstance()
            else -> null
        } ?: return@OnNavigationItemSelectedListener false

        supportFragmentManager.beginTransaction()
                .replace(R.id.content, fragment)
                .commit()

        return@OnNavigationItemSelectedListener true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        if (savedInstanceState == null) {
            navigation.selectedItemId = R.id.navigation_my_feeds
        }
    }

}
