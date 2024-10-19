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

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var resetPasswordButton: MaterialButton
    private lateinit var emailInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Inicializando os elementos da UI
        resetPasswordButton = findViewById(R.id.resetPasswordButton)
        emailInput = findViewById(R.id.emailInput)

        // Configurando o clique do botão
        resetPasswordButton.setOnClickListener {
            val email = emailInput.text.toString()
            // Lógica para enviar instruções de redefinição de senha
            if (email.isNotEmpty()) {
                // Adicione aqui a lógica para enviar o e-mail
                Toast.makeText(this, "Instruções enviadas para $email", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Por favor, insira um e-mail válido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun navigateToLogin(view: View) {
        // Navega de volta para a tela de login
        startActivity(Intent(this, LoginActivity::class.java))
    }
}
