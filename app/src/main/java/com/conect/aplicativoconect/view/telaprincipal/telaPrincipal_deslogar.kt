package com.conect.aplicativoconect.view.telaprincipal

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.conect.aplicativoconect.databinding.ActivityTelaPrincipalDeslogarBinding
import com.conect.aplicativoconect.view.telaLogin.telaLogin
import com.google.firebase.auth.FirebaseAuth

class telaPrincipal_deslogar : AppCompatActivity() {

    private lateinit var binding: ActivityTelaPrincipalDeslogarBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        binding = ActivityTelaPrincipalDeslogarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botaoDeslogar.setOnClickListener{
            FirebaseAuth.getInstance().signOut() // deslogar o usuario da sessão e fazer retornar ao menu de login

            val voltar_tela_login = Intent(this,telaLogin::class.java)
            startActivity(voltar_tela_login)
            finish()
        }
    }
}