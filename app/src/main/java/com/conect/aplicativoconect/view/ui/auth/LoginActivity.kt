package com.conect.aplicativoconect.view.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.client.ClienteHomeActivity
import com.conect.aplicativoconect.view.ui.admin.AdminHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

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
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Falha na autenticação. Verifique suas credenciais.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun verifyUserType(userId: String) {
        db.collection("business").document(userId).get()
            .addOnSuccessListener { businessDocument ->
                if (businessDocument.exists()) {
                    // Empresa encontrada
                    saveFCMToken(userId, "business") // Salva o token para empresa
                    startActivity(Intent(this, AdminHomeActivity::class.java))
                    finish()
                } else {
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { userDocument ->
                            if (userDocument.exists()) {
                                val userType = userDocument.getString("type")
                                saveFCMToken(userId, "users") // Salva o token para cliente

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
