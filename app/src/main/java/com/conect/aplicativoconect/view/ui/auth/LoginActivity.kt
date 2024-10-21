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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button

    private var userType: String? = null // Variável para armazenar o tipo de usuário

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Referências aos componentes do layout
        emailEditText = findViewById(R.id.emailInput)
        passwordEditText = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signupButton)

        // Obter o tipo de usuário do Intent
        userType = intent.getStringExtra("USER_TYPE")

        // Lógica de login
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Por favor, insira email e senha", Toast.LENGTH_SHORT).show()
            }
        }

        // Botão de cadastro
        signUpButton.setOnClickListener {
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
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val db = FirebaseFirestore.getInstance()

                        // Verifique primeiro na coleção de usuários
                        val userRef = db.collection("users").document(userId)
                        userRef.get().addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val retrievedUserType = document.getString("type") // Obtém o tipo do usuário
                                val intent = if (retrievedUserType == "client") {
                                    Intent(this, ClienteHomeActivity::class.java)
                                } else {
                                    Intent(this, AdminHomeActivity::class.java)
                                }
                                startActivity(intent)
                                finish()
                            } else {
                                // Se não encontrar, tenta na coleção de negócios
                                val businessRef = db.collection("business").document(userId)
                                businessRef.get().addOnSuccessListener { businessDocument ->
                                    if (businessDocument != null && businessDocument.exists()) {
                                        // Se encontrar, assume que é um negócio
                                        val intent = Intent(this, AdminHomeActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                                    }
                                }.addOnFailureListener {
                                    Toast.makeText(this, "Erro ao recuperar dados do negócio", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this, "Erro ao recuperar dados do usuário", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Falha na autenticação.", Toast.LENGTH_SHORT).show()
                }
            }
    }

}
