package com.conect.aplicativoconect.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.conect.aplicativoconect.core.services.UpcomingBookingWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val workRequest =
                PeriodicWorkRequestBuilder<UpcomingBookingWorker>(30, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "UpcomingBookingWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}