package com.conect.aplicativoconect.view.ui.auth

import com.conect.aplicativoconect.view.ui.client.ClientHomeActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_login)

            // Inicializar Firebase Auth
            auth = FirebaseAuth.getInstance()

            // Referências aos componentes do layout
            emailEditText = findViewById(R.id.emailInput)
            passwordEditText = findViewById(R.id.passwordInput)
            loginButton = findViewById(R.id.loginButton)
            signUpButton = findViewById(R.id.signupButton)

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
                startActivity(Intent(this, SignupActivity::class.java))
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Erro ao inicializar: ${e.message}")
            Toast.makeText(this, "Erro ao inicializar a tela de login.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login bem-sucedido, redirecionar para a tela inicial
                    val intent = Intent(this, ClientHomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Se o login falhar, exibir mensagem ao usuário
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Falha na autenticação.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
