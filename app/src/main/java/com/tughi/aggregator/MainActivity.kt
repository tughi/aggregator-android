package com.tughi.aggregator

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tughi.aggregator.services.FeedUpdater

private const val PREF_ACTIVE_TAB = "active-tab"

private const val TAB_FEEDS = "feeds"
private const val TAB_MY_FEED = "my-feed"
private const val TAB_TAGS = "tags"

class MainActivity : AppCompatActivity() {

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val navigation by lazy { findViewById<BottomNavigationView>(R.id.navigation) }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        var tabName: String = TAB_FEEDS
        val fragment = when (item.itemId) {
            R.id.navigation_feeds -> FeedListFragment.newInstance().also { tabName = TAB_FEEDS }
            R.id.navigation_my_feeds -> MyFeedFragment.newInstance().also { tabName = TAB_MY_FEED }
            R.id.navigation_tags -> TagsFragment.newInstance().also { tabName = TAB_TAGS }
            else -> null
        } ?: return@OnNavigationItemSelectedListener false

        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
                .replace(R.id.content, fragment)
                .commit()

        preferences.edit()
                .putString(PREF_ACTIVE_TAB, tabName)
                .apply()

        return@OnNavigationItemSelectedListener true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        supportFragmentManager.let {
            it.addOnBackStackChangedListener {
                if (it.backStackEntryCount > 0) {
                    supportActionBar?.setHomeAsUpIndicator(R.drawable.action_back)
                } else {
                    supportActionBar?.setHomeAsUpIndicator(R.drawable.action_menu)
                    setTitle(R.string.app_name)
                }
            }
        }

        if (savedInstanceState == null) {
            FeedUpdater(this).update()

            navigation.selectedItemId = when (preferences.getString(PREF_ACTIVE_TAB, TAB_FEEDS)) {
                TAB_FEEDS -> R.id.navigation_feeds
                TAB_MY_FEED -> R.id.navigation_my_feeds
                TAB_TAGS -> R.id.navigation_tags
                else -> R.id.navigation_feeds
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.action_menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                // TODO: option navigation drawer
            }
        }

        return super.onOptionsItemSelected(item)
    }

}
