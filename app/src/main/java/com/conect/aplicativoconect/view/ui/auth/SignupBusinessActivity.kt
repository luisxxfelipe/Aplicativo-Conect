package com.conect.aplicativoconect.view.ui.auth

import RegisterBusinessActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    private fun createBusiness(email: String, password: String, businessName: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Cadastro bem-sucedido, salve os dados do negócio no Firestore
                    val businessId = auth.currentUser?.uid
                    val businessData = hashMapOf(
                        "email" to email,
                        "name" to businessName,
                        "isActive" to true,
                        "type" to "business"
                    )

                    businessId?.let {
                        db.collection("business").document(it).set(businessData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Cadastro de negócio bem-sucedido!", Toast.LENGTH_SHORT).show()
                                // Redirecionar para a tela de cadastro da empresa
                                startActivity(Intent(this, RegisterBusinessActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Falha ao salvar dados do negócio.", Toast.LENGTH_SHORT).show()
                            }
                    } ?: run {
                        // Caso businessId seja nulo
                        Toast.makeText(this, "Erro: ID de negócio não encontrado.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(baseContext, "Falha ao cadastrar. Tente novamente.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
