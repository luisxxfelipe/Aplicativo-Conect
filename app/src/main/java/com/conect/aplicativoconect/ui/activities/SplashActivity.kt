package com.conect.aplicativoconect.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.conect.aplicativoconect.core.services.PaymentService

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var paymentService: PaymentService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Inicializar Firebase Auth e Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        paymentService = PaymentService(this)

        // ✅ OTIMIZADO: Navegação paralela e mais rápida
        lifecycleScope.launch {
            delay(2000) // 2 segundos de delay para splash
            
            val currentUser = auth.currentUser
            if (currentUser != null) {
                try {
                    // ✅ QUERIES PARALELAS - Muito mais rápido!
                    val businessDeferred = async(Dispatchers.IO) {
                        firestore.collection("business").document(currentUser.uid).get().await()
                    }
                    val userDeferred = async(Dispatchers.IO) {
                        firestore.collection("users").document(currentUser.uid).get().await()
                    }

                    // Executar ambas queries ao mesmo tempo
                    val businessDoc = businessDeferred.await()
                    val userDoc = userDeferred.await()

                    when {
                        businessDoc.exists() -> {
                            // Usuário é um negócio - verificar assinatura
                            checkBusinessSubscription(currentUser.uid)
                        }
                        userDoc.exists() -> {
                            // Usuário é cliente - ir direto para home
                            startActivity(Intent(this@SplashActivity, ClienteHomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        }
                        else -> {
                            // Nenhum documento encontrado - usuário incompleto
                            startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    // Erro nas queries - redirecionar para welcome
                    startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
                    finish()
                }
            } else {
                // Usuário não logado
                startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
                finish()
            }
        }
    }
    
    /**
     * Verifica o status da assinatura do business user
     */
    private fun checkBusinessSubscription(userId: String) {
       // Tentar busca direta por document ID primeiro
        firestore.collection("subscriptions").document(userId)
            .get()
            .addOnSuccessListener { subscriptionDoc ->
                if (subscriptionDoc.exists()) {
                    processSubscriptionDoc(subscriptionDoc, userId)
                } else {
                    // Fallback: buscar por ownerId  
                    firestore.collection("subscriptions")
                        .whereEqualTo("ownerId", userId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            val foundDoc = if (!querySnapshot.isEmpty) querySnapshot.documents[0] else null
                            if (foundDoc != null && foundDoc.exists()) {
                                processSubscriptionDoc(foundDoc, userId)
                            } else {
                                startActivity(Intent(this@SplashActivity, PaymentActivity::class.java))
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            startActivity(Intent(this@SplashActivity, PaymentActivity::class.java))
                            finish()
                        }
                }
            }
            .addOnFailureListener { e ->
                // Se falhar, redirecionar para pagamento
                startActivity(Intent(this@SplashActivity, PaymentActivity::class.java))
                finish()
            }
    }
    
    private fun processSubscriptionDoc(subscriptionDoc: com.google.firebase.firestore.DocumentSnapshot, userId: String) {
        val subscriptionId = subscriptionDoc.id
        val status = subscriptionDoc.getString("status")
        val endDate = subscriptionDoc.getDate("endDate")
        val currentDate = java.util.Date()
        val lastPaymentId = subscriptionDoc.getString("paymentId")
        when {
            status == "active" && endDate != null && endDate.after(currentDate) -> {
                startActivity(Intent(this@SplashActivity, AdminHomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            status == "pending" -> {
                if (lastPaymentId != null) {
                    paymentService.validateSubscription(lastPaymentId) { isValid, expiryMillis ->
                        if (isValid) {
                            val update = hashMapOf(
                                "status" to "active",
                                "endDate" to (expiryMillis?.let { java.util.Date(it) }
                                    ?: java.util.Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)),
                                "updatedAt" to java.util.Date()
                            )
                            firestore.collection("subscriptions").document(subscriptionId)
                                .set(update, SetOptions.merge())
                                .addOnCompleteListener {
                                    startActivity(Intent(this@SplashActivity, AdminHomeActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                                    finish()
                                }
                        } else {
                            startActivity(Intent(this@SplashActivity, AdminHomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        }
                    }
                } else {
                    startActivity(Intent(this@SplashActivity, AdminHomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
            }
            else -> {
                startActivity(Intent(this@SplashActivity, PaymentActivity::class.java))
                finish()
            }
        }
    }
}
