package com.tughi.aggregator.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TopInsetsView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val barHeight = insets.top + insets.bottom

            if (barHeight <= 0) {
                view.visibility = GONE
            } else {
                view.layoutParams = view.layoutParams.apply { height = barHeight }
                view.visibility = VISIBLE
            }

            WindowInsetsCompat.CONSUMED
        }
    }
}

class BottomInsetsView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val barHeight = insets.top + insets.bottom

            if (barHeight <= 0) {
                view.visibility = GONE
            } else {
                view.layoutParams = view.layoutParams.apply { height = barHeight }
                view.visibility = VISIBLE
            }

            WindowInsetsCompat.CONSUMED
        }
    }
}
