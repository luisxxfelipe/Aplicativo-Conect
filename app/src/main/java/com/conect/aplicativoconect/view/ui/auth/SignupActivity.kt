package com.conect.aplicativoconect.view.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signUpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.emailInput)
        passwordEditText = findViewById(R.id.passwordInput)
        signUpButton = findViewById(R.id.signupButton)

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                createUser(email, password)
            } else {
                Toast.makeText(this, "Por favor, insira email e senha", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Cadastro bem-sucedido, redirecionar para tela de login
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(baseContext, "Falha ao cadastrar. Tente novamente.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
