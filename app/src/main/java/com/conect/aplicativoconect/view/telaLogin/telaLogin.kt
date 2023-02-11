package com.conect.aplicativoconect.view.telaLogin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.ActivityTelaLoginBinding
import com.conect.aplicativoconect.tela_cadastro

class telaLogin : AppCompatActivity() {

    private  lateinit var binding: ActivityTelaLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        binding = ActivityTelaLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textTelaCadastro.setOnClickListener{
            val intent = Intent(this,tela_cadastro::class.java)
            startActivity(intent)
        }
    }
}