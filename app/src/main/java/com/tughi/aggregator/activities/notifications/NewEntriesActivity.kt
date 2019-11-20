package com.tughi.aggregator.activities.notifications

import android.content.Intent
import android.os.Bundle
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.activities.main.MainActivity

class NewEntriesActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)

        finish()
    }

}
