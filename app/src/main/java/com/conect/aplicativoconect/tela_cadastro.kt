package com.conect.aplicativoconect

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.conect.aplicativoconect.databinding.ActivityTelaCadastroBinding
import com.google.android.material.snackbar.Snackbar

class tela_cadastro : AppCompatActivity() {
    private lateinit var binding: ActivityTelaCadastroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botaoCadastrar.setOnClickListener{view->
            val email = binding.cadastroEmail.text.toString()
            val senha = binding.cadastroSenha.text.toString()

            if(email.isEmpty() || senha.isEmpty()){
                val snackbar: Snackbar.make(it)
            }
        }
    }
}