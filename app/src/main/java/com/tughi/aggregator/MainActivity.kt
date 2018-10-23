package com.tughi.aggregator

import android.os.Bundle
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
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.content, fragment)
                .commit()

        App.preferences.edit()
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
        drawerNavigationView.inflateHeaderView(R.layout.drawer_header)
        drawerNavigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.dark_theme -> App.theme.value = APP_THEME_DARK
                R.id.light_theme -> App.theme.value = APP_THEME_LIGHT
            }
            return@setNavigationItemSelectedListener true
        }

        if (savedInstanceState == null) {
            FeedUpdater(this).update()

            bottomNavigationView.selectedItemId = when (App.preferences.getString(PREF_ACTIVE_TAB, TAB_FEEDS)) {
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

    override fun onBackPressed() {
        if (drawerView.isDrawerOpen(drawerNavigationView)) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }

    fun openDrawer() {
        drawerView.openDrawer(drawerNavigationView)
    }

    fun closeDrawer() {
        drawerView.closeDrawer(drawerNavigationView)
    }

}
