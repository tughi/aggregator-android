package com.tughi.aggregator.utilities

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.res.ResourcesCompat
import com.tughi.aggregator.R

fun Context.openURL(uri: Uri) {
    val customTabsIntent = CustomTabsIntent.Builder()
            .enableUrlBarHiding()
            .addDefaultShareMenuItem()
            .setToolbarColor(ResourcesCompat.getColor(resources, R.color.dark_theme__toolbar, null))
            .build()
    customTabsIntent.launchUrl(this, uri)
}

fun Context.openURL(url: String) {
    val uri = Uri.parse(url)
    openURL(uri)
}
