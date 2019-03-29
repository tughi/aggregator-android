package com.tughi.aggregator.widgets

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

fun EditText.makeClickable(listener: (View) -> Unit) {
    keyListener = null
    onFocusChangeListener = FocusToClickListener
    setOnClickListener { listener(it) }
}

private object FocusToClickListener : View.OnFocusChangeListener {
    override fun onFocusChange(view: View, hasFocus: Boolean) {
        if (hasFocus) {
            val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)

            view.callOnClick()
        }
    }
}
