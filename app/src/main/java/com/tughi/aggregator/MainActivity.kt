package com.tughi.aggregator

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_my_feeds -> {
                message.setText(R.string.title_my_feed)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_tags -> {
                message.setText(R.string.title_tags)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_feeds -> {
                message.setText(R.string.title_feeds)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }
}
