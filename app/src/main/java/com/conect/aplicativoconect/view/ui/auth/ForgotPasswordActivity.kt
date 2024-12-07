package com.conect.aplicativoconect.view.data.repository

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.auth.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var resetPasswordButton: MaterialButton
    private lateinit var emailInput: TextInputEditText
    private lateinit var auth: FirebaseAuth  // Referência para o FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Inicializando os elementos da UI
        resetPasswordButton = findViewById(R.id.resetPasswordButton)
        emailInput = findViewById(R.id.emailInput)

        // Inicializando o FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Configurando o clique do botão
        resetPasswordButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            // Verifica se o email foi informado
            if (email.isNotEmpty()) {
                // Lógica para enviar o e-mail de redefinição de senha via Firebase
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Por favor, insira um e-mail válido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        // Usando o FirebaseAuth para enviar o e-mail de redefinição de senha
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sucesso: E-mail enviado
                    Toast.makeText(this, "Instruções enviadas para $email", Toast.LENGTH_SHORT).show()

                    // Opcionalmente, redireciona o usuário de volta para o LoginActivity
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Erro: Exibe mensagem de erro
                    Toast.makeText(this, "Erro ao enviar o e-mail. Tente novamente.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun navigateToLogin(view: View) {
        // Navega de volta para a tela de login
        startActivity(Intent(this, LoginActivity::class.java))
    }
}
