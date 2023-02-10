package com.conect.aplicativoconect

import android.os.Binder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.conect.aplicativoconect.databinding.ActivityTelaBemVindoBinding

class tela_bem_vindo : AppCompatActivity() {
    private lateinit var binding: ActivityTelaBemVindoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTelaBemVindoBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}