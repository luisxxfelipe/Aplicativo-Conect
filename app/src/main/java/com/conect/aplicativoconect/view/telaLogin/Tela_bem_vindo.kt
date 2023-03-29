package com.conect.aplicativoconect.view.telaLogin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.conect.aplicativoconect.databinding.ActivityTelaBemVindoBinding
import com.conect.aplicativoconect.view.telaprincipal.tela_inicial
import com.google.firebase.auth.FirebaseAuth

class tela_bem_vindo : AppCompatActivity() {
    private lateinit var binding: ActivityTelaBemVindoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaBemVindoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.setaClique.setOnClickListener{
            val navegarSegundaTela = Intent(this, tela_bem_vindo2::class.java)
            startActivity(navegarSegundaTela)
        }

    }

    private fun navegarTelaPrincipal() {
        val intent = Intent(this, tela_inicial::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        val usuario_total = FirebaseAuth.getInstance().currentUser // usuario atual que esta logado no sistema

        if(usuario_total != null){
            navegarTelaPrincipal()
        }
    }
}