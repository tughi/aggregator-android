package com.tughi.aggregator.utilities

import android.app.Activity
import android.content.Intent
import com.tughi.aggregator.R

fun Activity.shareLink(link: String, title: String? = null) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, link)
    intent.putExtra(Intent.EXTRA_SUBJECT, title)
    intent.putExtra("sms_body", link)

    startActivity(Intent.createChooser(intent, getString(R.string.sharing__share_via)))
}
