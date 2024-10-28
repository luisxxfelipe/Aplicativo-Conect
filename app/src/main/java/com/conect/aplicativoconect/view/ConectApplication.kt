package com.conect.aplicativoconect.view

import android.app.Application
import com.google.firebase.FirebaseApp

class ConectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializando o Firebase automaticamente
        FirebaseApp.initializeApp(this)
    }
}
