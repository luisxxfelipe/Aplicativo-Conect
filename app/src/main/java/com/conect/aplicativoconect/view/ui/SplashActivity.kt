package com.conect.aplicativoconect.view.ui

import com.conect.aplicativoconect.view.ui.client.ClientHomeActivity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Navegar para a próxima tela após um delay
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser
            val intent = if (currentUser != null) {
                // Se o usuário estiver logado, redirecionar para a tela inicial
                Intent(this, ClientHomeActivity::class.java)
            } else {
                // Se o usuário não estiver logado, redirecionar para a tela de boas-vindas
                Intent(this, WelcomeActivity::class.java)
            }
            startActivity(intent)
            finish() // Finaliza a SplashActivity
        }, 2000) // Delay de 2 segundos
    }
}
