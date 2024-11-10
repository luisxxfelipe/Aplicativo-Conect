package com.conect.aplicativoconect.view.data

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.client.ClienteHomeActivity

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // Adicione um atraso antes de redirecionar para a próxima tela
        Handler().postDelayed({
            // Redireciona para a próxima tela (exemplo: Home ou Dashboard)
            startActivity(Intent(this, ClienteHomeActivity::class.java)) // Substitua conforme necessário
            finish() // Finaliza a LoadingActivity
        }, 3000) // 3000ms = 3 segundos (ajuste conforme necessário)
    }
}