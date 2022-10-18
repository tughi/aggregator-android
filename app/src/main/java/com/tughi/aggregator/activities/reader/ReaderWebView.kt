package com.tughi.aggregator.activities.reader

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class ReaderWebView(context: Context, attrs: AttributeSet) : WebView(context, attrs) {
    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        parent.requestDisallowInterceptTouchEvent(true)

        super.onScrollChanged(l, t, oldl, oldt)
    }
}
