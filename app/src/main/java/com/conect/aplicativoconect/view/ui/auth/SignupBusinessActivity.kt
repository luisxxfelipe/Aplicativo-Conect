package com.conect.aplicativoconect.view.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.admin.RegisterBusinessActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class SignupBusinessActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var businessNameEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var loginTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_business)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        emailEditText = findViewById(R.id.emailInput)
        passwordEditText = findViewById(R.id.passwordInput)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordInput)
        businessNameEditText = findViewById(R.id.businessNameInput)
        signUpButton = findViewById(R.id.signupButton)
        loginTextView = findViewById(R.id.loginTextView)

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val businessName = businessNameEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && businessName.isNotEmpty()) {
                if (password == confirmPassword) {
                    createBusiness(email, password, businessName)
                } else {
                    Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Navegar para a tela de login ao clicar no TextView
        loginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
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

    private fun createBusiness(email: String, password: String, businessName: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Recupera o UID do usuário autenticado
                    val user = auth.currentUser
                    val userId = user?.uid

                    if (userId != null) {
                        // Cria o objeto com os dados do negócio
                        val businessData = hashMapOf(
                            "email" to email,
                            "name" to businessName,
                            "isActive" to true,
                            "type" to "business",
                            "ownerId" to userId // UID do proprietário do negócio
                        )

                        saveFCMToken(userId, "business") // Salva o token para empresa

                        // Salva o negócio no Firestore
                        db.collection("business").document(userId)
                            .set(businessData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Cadastro de negócio bem-sucedido!", Toast.LENGTH_SHORT).show()
                                // Redireciona para a tela de registro de mais dados
                                val intent = Intent(this, RegisterBusinessActivity::class.java)
                                intent.putExtra("EMAIL_KEY", email)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e("CreateBusiness", "Erro ao salvar dados do negócio: ${e.message}")
                                Toast.makeText(this, "Falha ao salvar dados do negócio.", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Erro: UID do usuário não encontrado.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Exibe mensagem de erro se falhar ao criar a conta
                    Log.e("CreateBusiness", "Erro ao criar usuário: ${task.exception?.message}")
                    Toast.makeText(baseContext, "Falha ao cadastrar. Tente novamente.", Toast.LENGTH_SHORT).show()
                }
            }
    }

}
