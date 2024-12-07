package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.databinding.ActivityAppInfoBinding

class AppInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appVersionText.text = "Versão do App: 2.1.0"
        binding.appDeveloperText.text = "Desenvolvedor: ConecteX"
    }
}
