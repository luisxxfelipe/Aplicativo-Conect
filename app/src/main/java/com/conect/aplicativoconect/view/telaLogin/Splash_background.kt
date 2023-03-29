package com.conect.aplicativoconect.view.telaLogin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.conect.aplicativoconect.R

class splash_background : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_background)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, tela_bem_vindo::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}