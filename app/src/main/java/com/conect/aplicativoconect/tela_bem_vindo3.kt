package com.conect.aplicativoconect

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.conect.aplicativoconect.databinding.ActivityTelaBemVindo2Binding
import com.conect.aplicativoconect.databinding.ActivityTelaBemVindo3Binding
import com.conect.aplicativoconect.view.formCadastro.FormCadastro

class tela_bem_vindo3 : AppCompatActivity() {

    private lateinit var binding: ActivityTelaBemVindo3Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()
        binding = ActivityTelaBemVindo3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.setaClique.setOnClickListener {
            val navegarTelaInicial = Intent(this, FormCadastro::class.java)
            startActivity(navegarTelaInicial)
        }
    }
}