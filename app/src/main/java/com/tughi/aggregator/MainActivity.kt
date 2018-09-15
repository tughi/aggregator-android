package com.tughi.aggregator

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val fragment = when (item.itemId) {
            R.id.navigation_my_feeds -> MyFeedFragment.newInstance()
            R.id.navigation_tags -> TagsFragment.newInstance()
            R.id.navigation_feeds -> FeedsFragment.newInstance()
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
        navigation.selectedItemId = R.id.navigation_my_feeds
    }

}
