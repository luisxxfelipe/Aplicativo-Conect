package com.conect.aplicativoconect

import android.content.Intent
import android.os.Binder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.conect.aplicativoconect.databinding.ActivityTelaBemVindoBinding

class tela_bem_vindo : AppCompatActivity() {
    private lateinit var binding: ActivityTelaBemVindoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()
        binding = ActivityTelaBemVindoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.setaClique.setOnClickListener{
            val navegarSegundaTela = Intent(this,tela_bem_vindo2::class.java)
            startActivity(navegarSegundaTela)
        }

    }
}