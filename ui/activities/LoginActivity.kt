package com.conect.aplicativoconect.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.core.services.PaymentService
import com.conect.aplicativoconect.data.repositories.AuthRepository
import com.conect.aplicativoconect.utils.TokenManager
import com.conect.aplicativoconect.utils.Validator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

sealed class PaymentState {
    object Loading : PaymentState()
    data class Success(val message: String) : PaymentState()
    data class Error(val message: String) : PaymentState()
}

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button
    private val repo = AuthRepository()
    private val paymentService = PaymentService(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ... existing email/password setup ...

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (!Validator.isValidEmail(email)) {
                emailEditText.error = "Email inválido"
                return@setOnClickListener
            }
            if (password.length < 6) {
                passwordEditText.error = "Senha fraca"
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        // ... signUpButton, recoverPassword ...

    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    verifyUserType(userId, email)
                } else {
                    Toast.makeText(this, "Falha na autenticação.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun verifyUserType(userId: String, email: String) {
        lifecycleScope.launch {
            db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    lifecycleScope.launch {
                        TokenManager.saveToken("users", userId)
                    }
                    startActivity(Intent(this@LoginActivity, ClienteHomeActivity::class.java))
                    finish()
                } else {
                    // Business check moderno
                    val isSubscribed = repo.checkSubscription(userId)
                    if (isSubscribed) {
                        lifecycleScope.launch {
                            TokenManager.saveToken("business", userId)
                        }
                        startActivity(Intent(this@LoginActivity, AdminHomeActivity::class.java))
                        finish()
                    } else {
                        showExpiredSubscriptionDialog(email, userId)
                    }
                }
            }
        }
    }

    private fun showExpiredSubscriptionDialog(email: String, userId: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Assinatura Expirada")
            .setMessage("Renove para acessar o painel profissional (R$25/mês).")
            .setPositiveButton("Renovar") { _, _ ->
                initiatePayment(email, userId)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.roxo))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.roxo))
        }

        dialog.show()
    }

    private fun initiatePayment(email: String, userId: String) {
        // State loading moderno
        val loadingDialog = AlertDialog.Builder(this).setMessage("Processando pagamento...").setCancelable(false).create()
        loadingDialog.show()

        paymentService.createPayment(
            amount = 25.0,
            title = "Renovação Mensal Conect",
            email = email,
            onSuccess = { paymentId ->
                loadingDialog.dismiss()
                lifecycleScope.launch {
                    val renewed = repo.renewSubscription(userId, paymentId)
                    if (renewed) {
                        Toast.makeText(this@LoginActivity, "Renovado! Re-login...", Toast.LENGTH_SHORT).show()
                        reAuthenticate(email, passwordEditText.text.toString().trim(), userId)
                    } else {
                        Toast.makeText(this@LoginActivity, "Erro na renovação.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { error ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Erro: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun reAuthenticate(email: String, password: String, userId: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    verifyUserType(userId, email)  // Re-check e vai AdminHome
                } else {
                    Toast.makeText(this, "Re-login falhou.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // ... existing signUpButton, recoverPassword ...
}
