package com.conect.aplicativoconect.view

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.conect.aplicativoconect.view.ui.UpcomingBookingWorker
import com.google.firebase.FirebaseApp
import java.util.concurrent.TimeUnit

class ConectApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializando o Firebase automaticamente
        FirebaseApp.initializeApp(this)

        // Configurando o Worker para verificações de agendamentos uma vez ao iniciar o app
        setupUpcomingBookingWorker()
    }

    private fun setupUpcomingBookingWorker() {
        val workRequest = PeriodicWorkRequestBuilder<UpcomingBookingWorker>(30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "UpcomingBookingWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
