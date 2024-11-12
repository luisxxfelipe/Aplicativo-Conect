package com.conect.aplicativoconect.view.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.admin.AdminHomeActivity
import com.conect.aplicativoconect.view.ui.client.ClienteHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Inicializar Firebase Auth e Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Navegar para a próxima tela após um delay
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Primeiro, verificar se é um negócio
                firestore.collection("business").document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { businessDocument ->
                        if (businessDocument.exists()) {
                            // Usuário é um negócio
                            startActivity(Intent(this, AdminHomeActivity::class.java))
                            finish()
                        } else {
                            // Caso não seja "Business", verificar se é um Cliente
                            firestore.collection("users").document(currentUser.uid)
                                .get()
                                .addOnSuccessListener { userDocument ->
                                    if (userDocument.exists()) {
                                        // Usuário é um Cliente
                                        startActivity(Intent(this, ClienteHomeActivity::class.java))
                                    } else {
                                        // Documento do usuário não encontrado, redirecionar para a tela de boas-vindas
                                        startActivity(Intent(this, WelcomeActivity::class.java))
                                    }
                                    finish()
                                }
                                .addOnFailureListener {
                                    // Em caso de erro, redirecionar para a tela de boas-vindas
                                    startActivity(Intent(this, WelcomeActivity::class.java))
                                    finish()
                                }
                        }
                    }
                    .addOnFailureListener {
                        // Em caso de erro ao verificar negócios, também redirecionar para a tela de boas-vindas
                        startActivity(Intent(this, WelcomeActivity::class.java))
                        finish()
                    }
            } else {
                // Se o usuário não estiver logado, redirecionar para a tela de boas-vindas
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            }
        }, 2000) // Delay de 2 segundos
    }
}
