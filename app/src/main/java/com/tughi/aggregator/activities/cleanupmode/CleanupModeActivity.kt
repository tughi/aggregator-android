package com.tughi.aggregator.activities.cleanupmode

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
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
        private const val EXTRA_CLEANUP_MODE = "cleanup-mode"
        private const val EXTRA_SHOW_DEFAULT = "show-default"
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.cleanup_mode_activity, menu)
        saveMenuItem = menu.findItem(R.id.save)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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

    data class PickCleanupModeRequest(val currentCleanupMode: CleanupMode, val showDefault: Boolean = true)

    class PickCleanupMode : ActivityResultContract<PickCleanupModeRequest, CleanupMode?>() {
        override fun createIntent(context: Context, input: PickCleanupModeRequest): Intent =
            Intent(context, CleanupModeActivity::class.java)
                .putExtra(EXTRA_CLEANUP_MODE, input.currentCleanupMode.serialize())
                .putExtra(EXTRA_SHOW_DEFAULT, input.showDefault)

        override fun parseResult(resultCode: Int, intent: Intent?): CleanupMode? {
            if (resultCode != RESULT_OK) {
                return null
            }
            return CleanupMode.deserialize(intent?.getStringExtra(EXTRA_CLEANUP_MODE))
        }
    }

}

