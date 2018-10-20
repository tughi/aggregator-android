package com.tughi.aggregator

import android.os.Bundle
import androidx.appcompat.widget.Toolbar

class SubscribeActivity : AppActivity() {

    companion object {
        const val EXTRA_VIA_ACTION = "via_action"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.subscribe_activity)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setNavigationIcon(when (intent.getBooleanExtra(EXTRA_VIA_ACTION, false)) {
            true -> R.drawable.action_back
            false -> R.drawable.action_cancel
        })
        toolbar.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.content, SubscribeSearchFragment())
                    .commit()
        }
    }

}
