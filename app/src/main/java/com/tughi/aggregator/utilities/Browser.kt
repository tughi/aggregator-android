package com.tughi.aggregator.utilities

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.res.ResourcesCompat
import com.tughi.aggregator.R

fun Context.openURL(uri: Uri) {
    val customTabsIntent = CustomTabsIntent.Builder()
        .setUrlBarHidingEnabled(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ResourcesCompat.getColor(resources, R.color.dark_theme__toolbar, null))
                .build()
        )
        .build()
    customTabsIntent.launchUrl(this, uri)
}

fun Context.openURL(url: String) {
    val uri = Uri.parse(url)
    openURL(uri)
}
