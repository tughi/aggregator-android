package com.tughi.aggregator.activities.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
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

    private val bottomSheetBehavior by lazy { BottomSheetBehavior.from(bottomSheetView) }
    private val bottomSheetView by lazy { findViewById<View>(R.id.bottom_sheet) }
    private val bottomNavigationView by lazy { bottomSheetView.findViewById<BottomNavigationView>(R.id.bottom_navigation) }
    private val scrimView by lazy { findViewById<View>(R.id.scrim) }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        var tabName: String = TAB_FEEDS
        val fragment = when (item.itemId) {
            R.id.navigation_feeds -> FeedListFragment.newInstance().also { tabName = TAB_FEEDS }
            R.id.navigation_my_feeds -> MyFeedFragment.newInstance().also { tabName = TAB_MY_FEED }
            R.id.navigation_tags -> TagsFragment.newInstance().also { tabName = TAB_TAGS }
            else -> null
        } ?: return@OnNavigationItemSelectedListener false

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

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

        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffsey: Float) {
                scrimView.alpha = slideOffsey
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    scrimView.visibility = View.GONE
                } else {
                    scrimView.visibility = View.VISIBLE
                }
            }
        })

        scrimView.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        val onThemeClickListener = View.OnClickListener {
            when (it.id) {
                R.id.theme_accent_blue -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.BLUE)
                R.id.theme_accent_green -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.GREEN)
                R.id.theme_accent_orange -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.ORANGE)
                R.id.theme_accent_purple -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.PURPLE)
                R.id.theme_accent_red -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.RED)
                R.id.theme_dark -> App.style.value = App.style.value?.copy(theme = App.Style.Theme.DARK)
                R.id.theme_light -> App.style.value = App.style.value?.copy(theme = App.Style.Theme.LIGHT)
                R.id.theme_bottom_navigation_accent -> App.style.value = App.style.value?.copy(navigationBar = App.Style.NavigationBar.ACCENT)
                R.id.theme_bottom_navigation_gray -> App.style.value = App.style.value?.copy(navigationBar = App.Style.NavigationBar.GRAY)
            }
        }

        bottomSheetView.findViewById<View>(R.id.theme_accent_blue).setOnClickListener(onThemeClickListener)
        bottomSheetView.findViewById<View>(R.id.theme_accent_green).setOnClickListener(onThemeClickListener)
        bottomSheetView.findViewById<View>(R.id.theme_accent_orange).setOnClickListener(onThemeClickListener)
        bottomSheetView.findViewById<View>(R.id.theme_accent_purple).setOnClickListener(onThemeClickListener)
        bottomSheetView.findViewById<View>(R.id.theme_accent_red).setOnClickListener(onThemeClickListener)
        bottomSheetView.findViewById<View>(R.id.theme_bottom_navigation_accent).setOnClickListener(onThemeClickListener)
        bottomSheetView.findViewById<View>(R.id.theme_bottom_navigation_gray).setOnClickListener(onThemeClickListener)
        bottomSheetView.findViewById<View>(R.id.theme_dark).setOnClickListener(onThemeClickListener)
        bottomSheetView.findViewById<View>(R.id.theme_light).setOnClickListener(onThemeClickListener)

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
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    fun openDrawer() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

}
