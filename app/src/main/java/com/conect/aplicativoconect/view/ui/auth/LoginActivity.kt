package com.conect.aplicativoconect.view.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.PaymentService
import com.conect.aplicativoconect.view.ui.client.ClienteHomeActivity
import com.conect.aplicativoconect.view.ui.admin.AdminHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.emailInput)
        passwordEditText = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signupButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Por favor, insira email e senha", Toast.LENGTH_SHORT).show()
            }
        }

        signUpButton.setOnClickListener {
            val userType = intent.getStringExtra("USER_TYPE") ?: "client" // Padrão para cliente

            val intent = if (userType == "client") {
                Intent(this, SignupClientActivity::class.java) // Cadastro de cliente
            } else {
                Intent(this, SignupBusinessActivity::class.java) // Cadastro de empresa
            }
            startActivity(intent)
        }

    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    verifyUserType(userId)
                } else {
                    Toast.makeText(this, "Falha na autenticação. Verifique suas credenciais.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun verifyUserType(userId: String) {
        db.collection("business").document(userId).get()
            .addOnSuccessListener { businessDocument ->
                if (businessDocument.exists()) {
                    checkSubscriptionStatus(userId) { isSubscriptionValid ->
                        if (isSubscriptionValid) {
                            saveFCMToken(userId, "business")
                            startActivity(Intent(this, AdminHomeActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Assinatura expirada. Renove para continuar.", Toast.LENGTH_LONG).show()
                            FirebaseAuth.getInstance().signOut()  // Desloga o usuário imediatamente
                            initiatePayment(userId)
                        }
                    }
                } else {
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { userDocument ->
                            if (userDocument.exists()) {
                                val userType = userDocument.getString("type")
                                saveFCMToken(userId, "users")

                                val intent = if (userType == "client") {
                                    Intent(this, ClienteHomeActivity::class.java)
                                } else {
                                    Intent(this, AdminHomeActivity::class.java)
                                }
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("LoginActivity", "Erro ao buscar usuário: ", e)
                            Toast.makeText(this, "Erro ao recuperar dados do usuário.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Erro ao buscar empresa: ", e)
                Toast.makeText(this, "Erro ao recuperar dados do negócio.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun initiatePayment(userId: String) {
        FirebaseAuth.getInstance().signOut()  // Desloga o usuário imediatamente para garantir que não terá acesso à área de membros

        CoroutineScope(Dispatchers.Main).launch {
            val paymentService = PaymentService(this@LoginActivity)
            paymentService.createPayment(
                amount = 25.0f,
                title = "Assinatura Mensal",
                payerEmail = auth.currentUser?.email ?: "",
                onSuccess = {
                    // Atualizar status da assinatura no Firestore após o pagamento
                    db.collection("subscriptions").document(userId).update("isActive", true)
                    Toast.makeText(this@LoginActivity, "Pagamento bem-sucedido", Toast.LENGTH_SHORT).show()

                    // Re-autenticar o usuário após o pagamento bem-sucedido
                    auth.signInWithEmailAndPassword(emailEditText.text.toString().trim(), passwordEditText.text.toString().trim())
                        .addOnCompleteListener { signInTask ->
                            if (signInTask.isSuccessful) {
                                startActivity(Intent(this@LoginActivity, AdminHomeActivity::class.java))
                            } else {
                                Toast.makeText(this@LoginActivity, "Erro ao re-autenticar após pagamento.", Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                onError = { errorMessage ->
                    Toast.makeText(this@LoginActivity, "Erro no pagamento: $errorMessage", Toast.LENGTH_LONG).show()
                    // Redireciona para a tela de login após falha no pagamento
                    startActivity(Intent(this@LoginActivity, LoginActivity::class.java))
                }
            )
        }
    }


    // Função para verificar o status da assinatura
    private fun checkSubscriptionStatus(userId: String, callback: (Boolean) -> Unit) {
        db.collection("subscriptions").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val endDate = document.getTimestamp("endDate")?.toDate()
                    val isTrialActive = document.getBoolean("isTrialActive") ?: true
                    val isActive = document.getBoolean("isActive") ?: false

                    // Verifica se a assinatura é válida: se está no trial ou ativa com validade
                    val isSubscriptionValid = (isTrialActive && endDate?.after(Date()) == true) || isActive

                    // Se o trial expirou, atualiza para não ativo
                    if (isTrialActive && endDate?.before(Date()) == true) {
                        db.collection("subscriptions").document(userId)
                            .update("isTrialActive", false, "isActive", false) // define como inativo
                            .addOnSuccessListener {
                                Log.d("Subscription", "Período de teste expirado e atualizado.")
                                callback(false)  // Agora o callback será falso após o trial expirar
                            }
                            .addOnFailureListener { e ->
                                Log.e("Subscription", "Erro ao atualizar assinatura: ${e.message}")
                                callback(false)
                            }
                    } else {
                        callback(isSubscriptionValid)
                    }
                } else {
                    callback(false) // Sem assinatura
                }
            }
            .addOnFailureListener { e ->
                Log.e("SubscriptionCheck", "Erro ao verificar assinatura: ${e.message}")
                callback(false)
            }
    }


    // Função para obter e salvar o token FCM
    private fun saveFCMToken(userId: String, collection: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                db.collection(collection).document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "Token salvo com sucesso para $userId.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "Erro ao salvar token: ${e.message}")
                    }
            } else {
                Log.e("FCM", "Erro ao obter token", task.exception)
            }
        }
    }

}
