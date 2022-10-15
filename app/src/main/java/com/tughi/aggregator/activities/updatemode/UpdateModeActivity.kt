package com.tughi.aggregator.activities.updatemode

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
import com.tughi.aggregator.data.AdaptiveUpdateMode
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.DisabledUpdateMode
import com.tughi.aggregator.data.Every15MinutesUpdateMode
import com.tughi.aggregator.data.Every2HoursUpdateMode
import com.tughi.aggregator.data.Every30MinutesUpdateMode
import com.tughi.aggregator.data.Every3HoursUpdateMode
import com.tughi.aggregator.data.Every45MinutesUpdateMode
import com.tughi.aggregator.data.Every4HoursUpdateMode
import com.tughi.aggregator.data.Every6HoursUpdateMode
import com.tughi.aggregator.data.Every8HoursUpdateMode
import com.tughi.aggregator.data.EveryHourUpdateMode
import com.tughi.aggregator.data.OnAppLaunchUpdateMode
import com.tughi.aggregator.data.UpdateMode

class UpdateModeActivity : AppActivity() {

    companion object {
        private const val EXTRA_UPDATE_MODE = "update-mode"
        private const val EXTRA_SHOW_DEFAULT = "show-default"
    }

    private lateinit var adapter: UpdateModeAdapter

    private lateinit var saveMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_cancel)
        }

        setContentView(R.layout.update_mode_activity)

        val currentUpdateMode = UpdateMode.deserialize(intent.getStringExtra(EXTRA_UPDATE_MODE) ?: "")

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        val updateModes = mutableListOf(
            DisabledUpdateMode,
            AdaptiveUpdateMode,
            OnAppLaunchUpdateMode,
            Every15MinutesUpdateMode,
            Every30MinutesUpdateMode,
            Every45MinutesUpdateMode,
            EveryHourUpdateMode,
            Every2HoursUpdateMode,
            Every3HoursUpdateMode,
            Every4HoursUpdateMode,
            Every6HoursUpdateMode,
            Every8HoursUpdateMode
        )
        if (intent.getBooleanExtra(EXTRA_SHOW_DEFAULT, false)) {
            updateModes.add(0, DefaultUpdateMode)
        }

        adapter = UpdateModeAdapter(updateModes, currentUpdateMode, object : UpdateModeAdapter.Listener {
            override fun onUpdateModeClicked(updateMode: UpdateMode) {
                adapter.selectedUpdateMode = updateMode
            }
        })
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.update_mode_activity, menu)
        saveMenuItem = menu.findItem(R.id.save)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // ignored
            }
            R.id.save -> {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_UPDATE_MODE, adapter.selectedUpdateMode.serialize()))
            }
            else -> return super.onOptionsItemSelected(item)
        }

        finish()
        return true
    }

    data class PickUpdateModeRequest(val currentUpdateMode: UpdateMode, val showDefault: Boolean = true)

    class PickUpdateMode : ActivityResultContract<PickUpdateModeRequest, UpdateMode?>() {
        override fun createIntent(context: Context, input: PickUpdateModeRequest): Intent =
            Intent(context, UpdateModeActivity::class.java)
                .putExtra(EXTRA_UPDATE_MODE, input.currentUpdateMode.serialize())
                .putExtra(EXTRA_SHOW_DEFAULT, input.showDefault)

        override fun parseResult(resultCode: Int, intent: Intent?): UpdateMode? {
            if (resultCode == RESULT_OK) {
                val serializedUpdateMode = intent?.getStringExtra(EXTRA_UPDATE_MODE) ?: return null
                return UpdateMode.deserialize(serializedUpdateMode)
            }
            return null
        }
    }

}

