package com.tughi.aggregator.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.tughi.aggregator.R
import kotlin.math.min

class BottomSheetOption(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private val bottomNavigationHeight: Int
    private val maxWidth: Int

    init {
        val resources = context.resources
        bottomNavigationHeight = resources.getDimensionPixelSize(R.dimen.bottom_navigation_item_height)
        maxWidth = resources.getDimensionPixelSize(R.dimen.bottom_navigation_item_max_width)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        assert(widthMode == MeasureSpec.AT_MOST)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        assert(heightMode == MeasureSpec.AT_MOST)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        setMeasuredDimension(min(widthSize / 3, maxWidth), bottomNavigationHeight)
    }
}
