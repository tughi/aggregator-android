package com.tughi.aggregator.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.tughi.aggregator.R

/**
 * A specialized layout for entry lists.
 */
class EntryItemLayout(context: Context, attrs: AttributeSet) : ViewGroup(context, attrs) {

    private lateinit var authorView: View

    private lateinit var faviconView: View
    private lateinit var faviconLayoutParams: ViewGroup.MarginLayoutParams

    private lateinit var feedView: View

    private lateinit var selectorView: View

    private lateinit var starView: View
    private lateinit var starLayoutParams: ViewGroup.MarginLayoutParams

    private lateinit var timeView: View
    private lateinit var timeLayoutParams: ViewGroup.MarginLayoutParams

    private lateinit var titleView: View
    private lateinit var titleLayoutParams: ViewGroup.MarginLayoutParams

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        when (child.id) {
            R.id.author -> authorView = child
            R.id.favicon -> {
                faviconView = child
                faviconLayoutParams = params as ViewGroup.MarginLayoutParams
            }
            R.id.feed_title -> feedView = child
            R.id.selector -> selectorView = child
            R.id.star -> {
                starView = child
                starLayoutParams = params as ViewGroup.MarginLayoutParams
            }
            R.id.time -> {
                timeView = child
                timeLayoutParams = params as ViewGroup.MarginLayoutParams
            }
            R.id.title -> {
                titleView = child
                titleLayoutParams = params as ViewGroup.MarginLayoutParams
            }
        }

        super.addView(child, index, params)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight

        val measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        var measuredHeight = paddingTop + paddingBottom

        val maxLineWidth = measuredWidth - paddingLeft - paddingRight - titleLayoutParams.marginStart

        // first line

        starView.measure(View.MeasureSpec.makeMeasureSpec(starLayoutParams.width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(starLayoutParams.height, View.MeasureSpec.EXACTLY))
        timeView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

        var feedMaxWidth = maxLineWidth
        if (starView.visibility != View.GONE) {
            feedMaxWidth -= starLayoutParams.marginStart + starView.measuredWidth + starLayoutParams.marginEnd
        }
        feedMaxWidth -= timeLayoutParams.marginStart + timeView.measuredWidth + timeLayoutParams.marginEnd
        feedView.measure(View.MeasureSpec.makeMeasureSpec(feedMaxWidth, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        measuredHeight += feedView.measuredHeight

        // second line

        titleView.measure(View.MeasureSpec.makeMeasureSpec(maxLineWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        measuredHeight += titleView.measuredHeight

        faviconView.measure(View.MeasureSpec.makeMeasureSpec(faviconLayoutParams.width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(faviconLayoutParams.height, View.MeasureSpec.EXACTLY))

        // third line

        if (authorView.visibility != View.GONE) {
            authorView.measure(View.MeasureSpec.makeMeasureSpec(maxLineWidth, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            measuredHeight += authorView.measuredHeight
        }

        // selectors

        selectorView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(measuredHeight, View.MeasureSpec.EXACTLY))

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val rtl = layoutDirection == View.LAYOUT_DIRECTION_RTL

        val width = right - left
        val height = bottom - top
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight

        val titleLeft = paddingLeft + titleLayoutParams.marginStart

        // first line

        val firstLineLeft = titleLeft
        var firstLineRight = width - paddingRight
        val firstLineTop = paddingTop
        val firstLineBottom = firstLineTop + feedView.measuredHeight
        val firstLineBaseline = firstLineTop + feedView.baseline

        if (rtl) {
            feedView.layout(width - firstLineLeft - feedView.measuredWidth, firstLineTop, width - firstLineLeft, firstLineBottom)
        } else {
            feedView.layout(firstLineLeft, firstLineTop, firstLineLeft + feedView.measuredWidth, firstLineBottom)
        }

        firstLineRight -= timeLayoutParams.marginEnd

        val timeLeft = firstLineRight - timeView.measuredWidth
        val timeTop = firstLineBaseline - timeView.baseline
        if (rtl) {
            timeView.layout(width - firstLineRight, firstLineTop, width - timeLeft, timeTop + feedView.measuredHeight)
        } else {
            timeView.layout(timeLeft, firstLineTop, firstLineRight, timeTop + feedView.measuredHeight)
        }

        firstLineRight = timeLeft - timeLayoutParams.marginStart

        if (starView.visibility != View.GONE) {
            firstLineRight -= starLayoutParams.marginEnd

            val pinLeft = firstLineRight - starView.measuredWidth
            val pinTop = firstLineBaseline - starView.baseline
            if (rtl) {
                starView.layout(width - firstLineRight, pinTop, width - pinLeft, pinTop + starView.measuredHeight)
            } else {
                starView.layout(pinLeft, pinTop, firstLineRight, pinTop + starView.measuredHeight)
            }
        }

        // second line

        val titleBaseline = firstLineBottom + titleView.baseline
        val titleBottom = firstLineBottom + titleView.measuredHeight
        if (rtl) {
            titleView.layout(width - titleLeft - titleView.measuredWidth, firstLineBottom, width - titleLeft, titleBottom)
        } else {
            titleView.layout(titleLeft, firstLineBottom, titleLeft + titleView.measuredWidth, titleBottom)
        }

        val faviconTop = titleBaseline - faviconView.baseline
        if (rtl) {
            faviconView.layout(width - paddingLeft - faviconView.measuredWidth, faviconTop, width - paddingLeft, faviconTop + faviconView.measuredHeight)
        } else {
            faviconView.layout(paddingLeft, faviconTop, paddingLeft + faviconView.measuredWidth, faviconTop + faviconView.measuredHeight)
        }

        // third line

        if (authorView.visibility != View.GONE) {
            if (rtl) {
                authorView.layout(width - titleLeft - authorView.measuredWidth, titleBottom, width - titleLeft, titleBottom + authorView.measuredHeight)
            } else {
                authorView.layout(titleLeft, titleBottom, titleLeft + authorView.measuredWidth, titleBottom + authorView.measuredHeight)
            }
        }

        // selectors

        if (rtl) {
            selectorView.layout(width - selectorView.measuredWidth, 0, width, height)
        } else {
            selectorView.layout(0, 0, selectorView.measuredWidth, height)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): ViewGroup.LayoutParams {
        return ViewGroup.MarginLayoutParams(context, attrs)
    }

}
