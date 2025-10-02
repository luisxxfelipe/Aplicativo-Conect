package com.conect.aplicativoconect.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
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

        // Navegar para a próxima tela após um delay usando coroutines
        lifecycleScope.launch {
            delay(2000) // 2 segundos de delay
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Primeiro, verificar se é um negócio
                firestore.collection("business").document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { businessDocument ->
                        if (businessDocument.exists()) {
                            // Usuário é um negócio - verificar assinatura
                            checkBusinessSubscription(currentUser.uid)
                        } else {
                            // Caso não seja "Business", verificar se é um Cliente
                            firestore.collection("users").document(currentUser.uid)
                                .get()
                                .addOnSuccessListener { userDocument ->
                                    if (userDocument.exists()) {
                                        // Usuário é um Cliente - não precisa de assinatura
                                        startActivity(Intent(this@SplashActivity, ClienteHomeActivity::class.java))
                                    } else {
                                        // Documento do usuário não encontrado, redirecionar para a tela de boas-vindas
                                        startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
                                    }
                                    finish()
                                }
                                .addOnFailureListener {
                                    // Em caso de erro, redirecionar para a tela de boas-vindas
                                    startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
                                    finish()
                                }
                        }
                    }
                    .addOnFailureListener {
                        // Em caso de erro ao verificar negócios, também redirecionar para a tela de boas-vindas
                        startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
                        finish()
                    }
            } else {
                // Se o usuário não estiver logado, redirecionar para a tela de boas-vindas
                startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
                finish()
            }
        } // Fim da coroutine
    }
    
    /**
     * Verifica o status da assinatura do business user
     */
    private fun checkBusinessSubscription(userId: String) {
        firestore.collection("subscriptions").document(userId)
            .get()
            .addOnSuccessListener { subscriptionDoc ->
                if (subscriptionDoc.exists()) {
                    val status = subscriptionDoc.getString("status")
                    val endDate = subscriptionDoc.getDate("endDate")
                    val currentDate = java.util.Date()
                    
                    when {
                        status == "active" && endDate != null && endDate.after(currentDate) -> {
                            // Assinatura ativa e válida
                            startActivity(Intent(this@SplashActivity, AdminHomeActivity::class.java))
                            finish()
                        }
                        status == "pending" -> {
                            // Pagamento pendente - permitir acesso temporário
                            startActivity(Intent(this@SplashActivity, AdminHomeActivity::class.java))
                            finish()
                        }
                        else -> {
                            // Assinatura expirada ou inválida
                            startActivity(Intent(this@SplashActivity, PaymentActivity::class.java))
                            finish()
                        }
                    }
                } else {
                    // Nenhuma assinatura encontrada - primeiro uso
                    startActivity(Intent(this@SplashActivity, PaymentActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                // Em caso de erro, assumir que precisa de pagamento
                startActivity(Intent(this@SplashActivity, PaymentActivity::class.java))
                finish()
            }
    }
}
