package com.tughi.aggregator.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.tughi.aggregator.contentScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackupService : Service() {
    private val binder = LocalBinder()

    private var currentJob: Job? = null

    val status = MutableLiveData(Status(false))

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        Log.i(BackupService::class.java.simpleName, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val job = contentScope.launch {
            backup()
        }

        job.invokeOnCompletion {
            currentJob = null
            status.postValue(Status(false))
        }

        currentJob = job

        return START_NOT_STICKY
    }

    private suspend fun backup() {
        val duration = 13000
        val steps = 100L
        for (step in 1..steps) {
            status.postValue(Status(true, step / steps.toFloat()))
            delay(duration / steps)
        }
    }

    fun cancel() {
        currentJob?.cancel()
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@BackupService
    }

    data class Status(val busy: Boolean, val progress: Float = 0f)
}
