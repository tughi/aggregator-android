package com.tughi.aggregator.widgets

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Paint
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.tughi.aggregator.BuildConfig

/**
 * An [ImageView] that adjust its baseline based on the text appearance.
 */
class InlineImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    private val fontMetrics: Paint.FontMetricsInt

    init {

        var typedArray: TypedArray

        // get text appearance resource
        typedArray = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textAppearance))
        val textAppearance = typedArray.getResourceId(0, 0)
        typedArray.recycle()

        if (BuildConfig.DEBUG) {
            if (textAppearance == 0) {
                throw IllegalStateException("Missing 'android:textAppearance' attribute")
            }
        }

        // get text size
        typedArray = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textSize), 0, textAppearance)
        val textSize = typedArray.getDimension(0, -1f)
        typedArray.recycle()

        // get font metrics
        val paint = Paint()
        paint.textSize = textSize
        fontMetrics = paint.fontMetricsInt
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        val height = params.height

        if (BuildConfig.DEBUG) {
            // safe check
            if (height == ViewGroup.LayoutParams.MATCH_PARENT || height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                throw IllegalArgumentException("The provided height must be exact")
            }
        }

        val baseline = Math.round((height - fontMetrics.descent + fontMetrics.ascent) / 2f - fontMetrics.ascent)
        setBaseline(baseline)

        super.setLayoutParams(params)
    }

}
