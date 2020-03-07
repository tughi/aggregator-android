package com.tughi.aggregator.activities.subscribe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R

class SubscribeActivity : AppActivity() {

    companion object {
        private const val EXTRA_VIA_ACTION = "via_action"

        fun start(context: Context, viaAction: Boolean = false) {
            context.startActivity(
                    Intent(context, SubscribeActivity::class.java)
                            .putExtra(EXTRA_VIA_ACTION, viaAction)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(when (intent.getBooleanExtra(EXTRA_VIA_ACTION, false)) {
                true -> R.drawable.action_back
                false -> R.drawable.action_cancel
            })
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, SubscribeSearchFragment())
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

}
