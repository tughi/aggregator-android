package com.tughi.aggregator.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

class ReaderViewPager(context: Context, attrs: AttributeSet) : ViewPager(context, attrs) {

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val pageMargin = pageMargin
        super.onSizeChanged(w - pageMargin, h, oldw - pageMargin, oldh)
    }

}
