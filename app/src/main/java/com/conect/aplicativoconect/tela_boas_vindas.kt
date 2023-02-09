package com.conect.aplicativoconect

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.conect.aplicativoconect.databinding.ActivityTelaBoasVindasBinding

class tela_boas_vindas : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_background)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent= Intent(this,tela_boas_vindas2::class.java)
            startActivity(intent)
            finish()
        },3000)
    }
}