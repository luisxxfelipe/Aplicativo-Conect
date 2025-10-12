package com.conect.aplicativoconect.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.utils.TokenManager

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: com.google.firebase.auth.FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = com.google.firebase.auth.FirebaseAuth.getInstance()

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

        val recoverPasswordButton: Button = findViewById(R.id.recoverpassword)

        recoverPasswordButton.setOnClickListener {
            // Ao clicar no botão "Esqueci minha senha", vamos abrir a tela de recuperação
            val intent = Intent(this, ForgotPasswordActivity::class.java)
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
                    val exception = task.exception
                    val errorMessage = when {
                        exception?.message?.contains("password is invalid") == true || 
                        exception?.message?.contains("wrong-password") == true -> 
                            "Senha incorreta. Tente novamente ou use 'Esqueci minha senha'."
                        
                        exception?.message?.contains("user not found") == true ||
                        exception?.message?.contains("no user record") == true -> 
                            "Email não encontrado. Verifique o email ou cadastre-se."
                        
                        exception?.message?.contains("invalid email") == true -> 
                            "Email inválido. Verifique o formato do email."
                        
                        exception?.message?.contains("network error") == true -> 
                            "Erro de conexão. Verifique sua internet."
                            
                        else -> "Falha na autenticação: ${exception?.message ?: "Verifique suas credenciais"}"
                    }
                    
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun verifyUserType(userId: String) {
        // Primeiramente, verifica se o usuário está na coleção "users"
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    val userType = userDocument.getString("type") ?: "client"

                    // Usuário existe na collection "users" - sempre vai para ClienteHome
                    val intent = Intent(this, ClienteHomeActivity::class.java).apply {
                        putExtra("FORCE_UPDATE", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Caso o usuário não exista em "users", verifica em "business"
                    db.collection("business").document(userId).get()
                        .addOnSuccessListener { businessDocument ->
                            if (businessDocument.exists()) {
                                // Usuário é business - vai para AdminHome
                                startActivity(Intent(this@LoginActivity, AdminHomeActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                                finish()
                            } else {
                                // Usuário não existe nem em "users" nem em "business"
                                // Redirecionar para tela de boas-vindas para escolher tipo
                                Toast.makeText(this, "Complete seu cadastro para continuar.", Toast.LENGTH_SHORT)
                                    .show()
                                startActivity(Intent(this@LoginActivity, WelcomeActivity::class.java))
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Erro ao recuperar dados do negócio.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao recuperar dados do usuário.", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun reAuthenticateAndRedirect(userId: String) {
        auth.signInWithEmailAndPassword(
            emailEditText.text.toString().trim(),
            passwordEditText.text.toString().trim()
        ).addOnCompleteListener { signInTask ->
            if (signInTask.isSuccessful) {
                // Após reautenticar, verifica o tipo de usuário e redireciona
                verifyUserType(userId)  // Essa função já faz o redirecionamento para a home correta
            } else {
                Toast.makeText(
                    this@LoginActivity,
                    "Erro ao re-autenticar após pagamento.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


}
