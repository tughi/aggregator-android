package com.tughi.aggregator.activities.backup

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.services.BackupService
import java.util.Date
import kotlin.math.roundToInt

class BackupActivity : AppActivity() {
    private val createBackupDocumentRequest = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) {
            startService(
                Intent(this, BackupService::class.java).apply {
                    data = uri
                }
            )
        }
    }

    private var service: BackupService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as BackupService.LocalBinder).getService().also { service ->
                service.status.observe(this@BackupActivity) { status ->
                    serviceStatus.value = status
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    private val serviceStatus = MutableLiveData<BackupService.Status>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_back)
        }

        setContentView(R.layout.backup_activity)

        val actionsWrapper = findViewById<ViewGroup>(R.id.actions_wrapper)
        val backupButton = actionsWrapper.findViewById<Button>(R.id.backup)
        backupButton.setOnClickListener {
            createBackupDocumentRequest.launch(
                Date().let { date ->
                    "AggregatorData-%tY%tm%td%tH%tM%tS.ion.gz".format(date, date, date, date, date, date)
                }
            )
        }

        val progressWrapper = findViewById<ViewGroup>(R.id.progress_wrapper)
        val progressBar = progressWrapper.findViewById<ProgressBar>(R.id.progress)
        val cancelButton = progressWrapper.findViewById<Button>(R.id.cancel)
        cancelButton.setOnClickListener {
            service?.cancel()
        }

        serviceStatus.observe(this) { status ->
            if (status.busy) {
                actionsWrapper.visibility = View.GONE
                progressWrapper.visibility = View.VISIBLE
                progressBar.isIndeterminate = false
                progressBar.progress = (status.progress * 100).roundToInt()
            } else {
                actionsWrapper.visibility = View.VISIBLE
                progressWrapper.visibility = View.GONE
                progressBar.isIndeterminate = true
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Intent(this, BackupService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onPause() {
        unbindService(serviceConnection)

        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }
}
