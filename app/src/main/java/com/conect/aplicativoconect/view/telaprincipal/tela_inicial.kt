package com.conect.aplicativoconect.view.telaprincipal

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.lifecycle.createSavedStateHandle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.ActivityTelaInicialBinding
import com.conect.aplicativoconect.view.telaLogin.telaLogin
import com.conect.aplicativoconect.view.telaprincipal.dominio.CategoriasDominio
import com.google.firebase.auth.FirebaseAuth

class tela_inicial : AppCompatActivity() {

    private lateinit var binding: ActivityTelaInicialBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTelaInicialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.icSair.setOnClickListener{
            FirebaseAuth.getInstance().signOut() // deslogar o usuario da sessão e fazer retornar ao menu de login

            val voltar_tela_login = Intent(this, telaLogin::class.java)
            startActivity(voltar_tela_login)
            finish()
        }
    }
}