package com.conect.aplicativoconect.view.telaLogin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.conect.aplicativoconect.databinding.ActivityTelaBemVindo3Binding
import com.conect.aplicativoconect.view.telaprincipal.telaPrincipal
import com.google.firebase.auth.FirebaseAuth

class tela_bem_vindo3 : AppCompatActivity() {

    private lateinit var binding: ActivityTelaBemVindo3Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()
        binding = ActivityTelaBemVindo3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.setaClique.setOnClickListener {
            val navegarTelaInicial = Intent(this, telaLogin::class.java)
            startActivity(navegarTelaInicial)
        }
    }

    private fun navegarTelaPrincipal() {
        val intent = Intent(this, telaPrincipal::class.java)
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