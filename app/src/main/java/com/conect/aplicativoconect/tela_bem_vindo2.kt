package com.conect.aplicativoconect

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.conect.aplicativoconect.databinding.ActivityFormCadastroBinding
import com.conect.aplicativoconect.databinding.ActivityTelaBemVindo2Binding

class tela_bem_vindo2 : AppCompatActivity() {

    private lateinit var binding: ActivityTelaBemVindo2Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaBemVindo2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.setaClique.setOnClickListener {
            val navegarTerceiraTela = Intent(this, tela_bem_vindo3::class.java)
            startActivity(navegarTerceiraTela)
        }
    }
}