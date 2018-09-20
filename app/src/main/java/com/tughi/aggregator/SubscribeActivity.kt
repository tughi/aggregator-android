package com.tughi.aggregator

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SubscribeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VIA_ACTION = "via_action"
    }

    private val urlEditText by lazy { findViewById<EditText>(R.id.url) }
    private val searchButton by lazy { findViewById<Button>(R.id.search) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.subscribe_activity)

        // TODO: handle keyboard action and search button events
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            if (!intent.getBooleanExtra(EXTRA_VIA_ACTION, false)) {
                setHomeAsUpIndicator(R.drawable.action_cancel)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home ->
                finish()
            else ->
                return super.onOptionsItemSelected(item)
        }
        return true
    }

}
