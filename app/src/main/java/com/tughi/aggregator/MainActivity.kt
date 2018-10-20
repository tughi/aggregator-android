package com.tughi.aggregator

import android.os.Bundle
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.tughi.aggregator.services.FeedUpdater
import com.tughi.aggregator.utilities.APP_THEME_DARK
import com.tughi.aggregator.utilities.APP_THEME_LIGHT

private const val PREF_ACTIVE_TAB = "active-tab"

private const val TAB_FEEDS = "feeds"
private const val TAB_MY_FEED = "my-feed"
private const val TAB_TAGS = "tags"

class MainActivity : AppActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var drawerView: DrawerLayout
    private lateinit var drawerNavigationView: NavigationView

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

        Application.preferences.edit()
                .putString(PREF_ACTIVE_TAB, tabName)
                .apply()

        return@OnNavigationItemSelectedListener true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        drawerView = findViewById(R.id.drawer)

        bottomNavigationView = drawerView.findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        drawerNavigationView = drawerView.findViewById(R.id.drawer_navigation)
        drawerNavigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.dark_theme -> Application.theme.value = APP_THEME_DARK
                R.id.light_theme -> Application.theme.value = APP_THEME_LIGHT
            }
            return@setNavigationItemSelectedListener true
        }

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

            bottomNavigationView.selectedItemId = when (Application.preferences.getString(PREF_ACTIVE_TAB, TAB_FEEDS)) {
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
                if (drawerView.isDrawerOpen(drawerNavigationView)) {
                    drawerView.closeDrawer(drawerNavigationView)
                } else {
                    drawerView.openDrawer(drawerNavigationView)
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

}
