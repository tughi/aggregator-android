package com.tughi.aggregator.activities.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tughi.aggregator.App
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.BuildConfig
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.theme.ThemeActivity
import com.tughi.aggregator.preferences.UpdateSettings
import com.tughi.aggregator.preferences.User
import com.tughi.aggregator.services.FeedUpdateHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppActivity() {

    companion object {
        const val ACTION_VIEW_MY_FEED = BuildConfig.APPLICATION_ID + ".intent.action.VIEW_MY_FEED"

        private const val PREF_ACTIVE_TAB = "main__active_tab"

        private const val TAB_FEEDS = "feeds"
        private const val TAB_MY_FEED = "my-feed"
        private const val TAB_TAGS = "tags"

        private const val INSTANCE_STATE__BOTTOM_SHEET_EXPANDED = "bottom_sheet_expanded"
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

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
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

        bottomSheetView.findViewById<View>(R.id.theme).setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            startActivity(Intent(this, ThemeActivity::class.java))
        }

        bottomSheetView.findViewById<TextView>(R.id.version).text = BuildConfig.VERSION_NAME

        if (savedInstanceState == null) {
            val activeTab: String? = if (intent.action == ACTION_VIEW_MY_FEED) TAB_MY_FEED else App.preferences.getString(PREF_ACTIVE_TAB, null)

            bottomNavigationView.selectedItemId = when (activeTab) {
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
        } else {
            if (savedInstanceState.getBoolean(INSTANCE_STATE__BOTTOM_SHEET_EXPANDED, false)) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                scrimView.alpha = 1.toFloat()
                scrimView.visibility = View.VISIBLE
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.action == ACTION_VIEW_MY_FEED) {
            bottomNavigationView.selectedItemId = R.id.navigation_my_feeds
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(INSTANCE_STATE__BOTTOM_SHEET_EXPANDED, bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        User.lastSeen = System.currentTimeMillis()

        super.onDestroy()
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
