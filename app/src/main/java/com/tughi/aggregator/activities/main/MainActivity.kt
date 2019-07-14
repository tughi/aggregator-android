package com.tughi.aggregator.activities.main

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tughi.aggregator.App
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.preferences.UpdateSettings
import com.tughi.aggregator.services.FeedUpdateHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppActivity() {

    companion object {
        private const val PREF_ACTIVE_TAB = "active-tab"

        private const val TAB_FEEDS = "feeds"
        private const val TAB_MY_FEED = "my-feed"
        private const val TAB_TAGS = "tags"
    }

    private lateinit var bottomNavigationView: BottomNavigationView

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

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        // TODO: apply app style
        // when (it.itemId) {
        //     R.id.dark_theme -> App.style.value = App.style.value?.copy(theme = App.Style.Theme.DARK)
        //     R.id.light_theme -> App.style.value = App.style.value?.copy(theme = App.Style.Theme.LIGHT)
        //     R.id.accent_blue -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.BLUE)
        //     R.id.accent_green -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.GREEN)
        //     R.id.accent_orange -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.ORANGE)
        //     R.id.accent_purple -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.PURPLE)
        //     R.id.accent_red -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.RED)
        //     R.id.navigation_bar_style_accent -> App.style.value = App.style.value?.copy(navigationBar = App.Style.NavigationBar.ACCENT)
        //     R.id.navigation_bar_style_gray -> App.style.value = App.style.value?.copy(navigationBar = App.Style.NavigationBar.GRAY)
        // }

        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = when (App.preferences.getString(PREF_ACTIVE_TAB, TAB_FEEDS)) {
                TAB_FEEDS -> R.id.navigation_feeds
                TAB_MY_FEED -> R.id.navigation_my_feeds
                TAB_TAGS -> R.id.navigation_tags
                else -> R.id.navigation_feeds
            }

            if (UpdateSettings.backgroundUpdates) {
                GlobalScope.launch {
                    FeedUpdateHelper.updateOutdatedFeeds(true)
                }
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
        if (false /* TODO: check if bottom sheet is expanded */) {
            // TODO: collapse the bottom sheet
        } else {
            super.onBackPressed()
        }
    }

    fun openDrawer() {
        // TODO: expand the bottom sheet
    }

}
