package com.conect.aplicativoconect.view.telaLogin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.conect.aplicativoconect.R

class telaLogin : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()
        setContentView(R.layout.activity_tela_login)
    }
}