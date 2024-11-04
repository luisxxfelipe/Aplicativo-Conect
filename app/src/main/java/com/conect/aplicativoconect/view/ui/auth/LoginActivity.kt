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
import com.conect.aplicativoconect.view.ui.admin.AdminHomeActivity
import com.conect.aplicativoconect.view.ui.client.ClienteHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
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
                Intent(this, SignupClientActivity::class.java)
            } else {
                Intent(this, SignupBusinessActivity::class.java)
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
                    Toast.makeText(
                        this,
                        "Falha na autenticação. Verifique suas credenciais.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun verifyUserType(userId: String) {
        // Primeiramente, verifica se o usuário está na coleção "users"
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    val userType = userDocument.getString("type") ?: "client"
                    saveFCMToken(userId, "users")

                    // Redireciona com base no tipo do usuário
                    val intent = if (userType == "client") {
                        Intent(this, ClienteHomeActivity::class.java)
                    } else {
                        Intent(this, AdminHomeActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Caso o usuário não exista em "users", verifica em "business"
                    db.collection("business").document(userId).get()
                        .addOnSuccessListener { businessDocument ->
                            if (businessDocument.exists()) {
                                checkSubscriptionStatus(userId) { isSubscriptionValid ->
                                    if (isSubscriptionValid) {
                                        saveFCMToken(userId, "business")
                                        startActivity(Intent(this, AdminHomeActivity::class.java))
                                        finish()
                                    } else {
                                        // Mostra um diálogo de assinatura expirada
                                        FirebaseAuth.getInstance().signOut()
                                        showExpiredSubscriptionDialog(userId)
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("LoginActivity", "Erro ao buscar empresa: ", e)
                            Toast.makeText(
                                this,
                                "Erro ao recuperar dados do negócio.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Erro ao buscar usuário: ", e)
                Toast.makeText(this, "Erro ao recuperar dados do usuário.", Toast.LENGTH_SHORT)
                    .show()
            }
    }


    private fun showExpiredSubscriptionDialog(userId: String) {
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
        dialogBuilder.setTitle("Assinatura Expirada")
            .setMessage("Sua assinatura expirou. Deseja renovar para continuar?")
            .setPositiveButton("Renovar Assinatura") { _, _ ->
                initiatePayment(userId)  // Chama o pagamento
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                // Opcionalmente, você pode redirecionar o usuário para a tela de login novamente
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setCancelable(false) // Evita que o usuário feche o diálogo fora das opções dadas
            .show()
    }

    private fun initiatePayment(userId: String) {
        FirebaseAuth.getInstance().signOut()  // Desloga o usuário imediatamente

        CoroutineScope(Dispatchers.Main).launch {
            val paymentService = PaymentService(this@LoginActivity)
            paymentService.createPayment(
                amount = 25.0f,
                title = "Assinatura Mensal",
                payerEmail = auth.currentUser?.email ?: "",
                onSuccess = {
                    renewSubscription(userId)
                    Toast.makeText(this@LoginActivity, "Pagamento bem-sucedido", Toast.LENGTH_SHORT)
                        .show()

                    // Re-autenticar o usuário após o pagamento bem-sucedido
                    auth.signInWithEmailAndPassword(
                        emailEditText.text.toString().trim(),
                        passwordEditText.text.toString().trim()
                    )
                        .addOnCompleteListener { signInTask ->
                            if (signInTask.isSuccessful) {
                                startActivity(
                                    Intent(
                                        this@LoginActivity,
                                        AdminHomeActivity::class.java
                                    )
                                )
                                finish()
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Erro ao re-autenticar após pagamento.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                },
                onError = { errorMessage ->
                    Toast.makeText(
                        this@LoginActivity,
                        "Erro no pagamento: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this@LoginActivity, LoginActivity::class.java))
                }
            )
        }
    }

    private fun renewSubscription(userId: String) {
        db.collection("subscriptions").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentEndDate = document.getTimestamp("endDate")?.toDate() ?: Date()

                    // Calcula a nova data de expiração adicionando um mês
                    val calendar = Calendar.getInstance().apply {
                        time = currentEndDate
                        add(Calendar.MONTH, 1)
                    }
                    val newEndDate = calendar.time

                    // Atualiza a assinatura com a nova data de expiração e define como ativa
                    db.collection("subscriptions").document(userId)
                        .update("endDate", newEndDate, "isActive", true)
                        .addOnSuccessListener {
                            Log.d("Subscription", "Assinatura renovada com sucesso.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Subscription", "Erro ao renovar assinatura: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Subscription", "Erro ao acessar assinatura: ${e.message}")
            }
    }

    private fun checkSubscriptionStatus(userId: String, callback: (Boolean) -> Unit) {
        db.collection("subscriptions").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val endDate = document.getTimestamp("endDate")?.toDate()
                    val isTrialActive = document.getBoolean("isTrialActive") ?: true
                    val isActive = document.getBoolean("isActive") ?: false

                    val isSubscriptionValid =
                        (isTrialActive && endDate?.after(Date()) == true) || isActive

                    if (isTrialActive && endDate?.before(Date()) == true) {
                        db.collection("subscriptions").document(userId)
                            .update("isTrialActive", false, "isActive", false)
                            .addOnSuccessListener {
                                Log.d("Subscription", "Período de teste expirado e atualizado.")
                                callback(false)
                            }
                            .addOnFailureListener { e ->
                                Log.e("Subscription", "Erro ao atualizar assinatura: ${e.message}")
                                callback(false)
                            }
                    } else {
                        callback(isSubscriptionValid)
                    }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("SubscriptionCheck", "Erro ao verificar assinatura: ${e.message}")
                callback(false)
            }
    }

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
