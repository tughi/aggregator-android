package com.tughi.aggregator.activities.cleanupmode

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.data.Age1MonthCleanupMode
import com.tughi.aggregator.data.Age1WeekCleanupMode
import com.tughi.aggregator.data.Age1YearCleanupMode
import com.tughi.aggregator.data.Age3DaysCleanupMode
import com.tughi.aggregator.data.Age3MonthsCleanupMode
import com.tughi.aggregator.data.Age3YearsCleanupMode
import com.tughi.aggregator.data.Age6MonthsCleanupMode
import com.tughi.aggregator.data.Age6YearsCleanupMode
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.DefaultCleanupMode
import com.tughi.aggregator.data.NeverCleanupMode

class CleanupModeActivity : AppActivity() {

    companion object {
        const val EXTRA_CLEANUP_MODE = "cleanup-mode"
        const val EXTRA_SHOW_DEFAULT = "show-default"
    }

    private lateinit var adapter: CleanupModeAdapter

    private lateinit var saveMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_cancel)
        }

        setContentView(R.layout.cleanup_mode_activity)

        val currentCleanupMode = CleanupMode.deserialize(intent.getStringExtra(EXTRA_CLEANUP_MODE))

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        val cleanupModes = mutableListOf(
                NeverCleanupMode,
                Age3DaysCleanupMode,
                Age1WeekCleanupMode,
                Age1MonthCleanupMode,
                Age3MonthsCleanupMode,
                Age6MonthsCleanupMode,
                Age1YearCleanupMode,
                Age3YearsCleanupMode,
                Age6YearsCleanupMode
        )
        if (intent.getBooleanExtra(EXTRA_SHOW_DEFAULT, false)) {
            cleanupModes.add(0, DefaultCleanupMode)
        }

        adapter = CleanupModeAdapter(cleanupModes, currentCleanupMode, object : CleanupModeAdapter.Listener {
            override fun onCleanupModeClicked(cleanupMode: CleanupMode) {
                adapter.selectedCleanupMode = cleanupMode
            }
        })
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val result = super.onCreateOptionsMenu(menu)

        menu?.let {
            menuInflater.inflate(R.menu.cleanup_mode_activity, it)
            saveMenuItem = it.findItem(R.id.save)
            return true
        }

        return result
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                // ignored
            }
            R.id.save -> {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_CLEANUP_MODE, adapter.selectedCleanupMode.serialize()))
            }
            else -> return super.onOptionsItemSelected(item)
        }

        finish()
        return true
    }

}

