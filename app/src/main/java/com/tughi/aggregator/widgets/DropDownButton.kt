package com.tughi.aggregator.widgets

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.core.view.GestureDetectorCompat
import com.google.android.material.textfield.TextInputLayout
import com.tughi.aggregator.R

class DropDownButton(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val view = LayoutInflater.from(context).inflate(R.layout.drop_down_button, this, false) as TextInputLayout
    private val text = view.findViewById<EditText>(R.id.text)

    private val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            requestFocus()
            return performClick()
        }
    })

    init {
        text.keyListener = null

        addView(view)

        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true

        val styledAttributes = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.hint))
        view.hint = styledAttributes.getText(0)
        styledAttributes.recycle()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (child != view) {
            throw UnsupportedOperationException()
        }
        super.addView(child, index, params)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return true
    }

    private var detectedClick = false

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            detectedClick = true
            gestureDetector.onTouchEvent(event)
            detectedClick = false
        }

        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        if (detectedClick) {
            return super.performClick()
        }

        return false
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)

        if (gainFocus) {
            text.requestFocus()
        }
    }

    fun setText(@StringRes resId: Int) {
        text.setText(resId)
    }

    fun setText(text: CharSequence) {
        this.text.setText(text)
    }

}
