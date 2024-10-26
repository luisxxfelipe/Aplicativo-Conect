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
            val intent = Intent(this, SignupClientActivity::class.java)
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
        // Verificar primeiro se é um negócio
        db.collection("business").document(userId).get()
            .addOnSuccessListener { businessDocument ->
                if (businessDocument != null && businessDocument.exists()) {
                    // Se encontrar, redireciona para a tela de administração
                    val intent = Intent(this, AdminHomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Caso não seja um negócio, verifica se é um cliente
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { userDocument ->
                            if (userDocument != null && userDocument.exists()) {
                                val userType = userDocument.getString("type")
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
                Log.e("LoginActivity", "Erro ao buscar negócio: ", e)
                Toast.makeText(this, "Erro ao recuperar dados do negócio.", Toast.LENGTH_SHORT).show()
            }
    }
}
