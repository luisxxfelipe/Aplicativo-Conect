package com.conect.aplicativoconect.view.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.admin.RegisterBusinessActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Calendar
import android.provider.Settings.Secure
import com.google.firebase.auth.FirebaseAuthWeakPasswordException


class SignupBusinessActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var nameUserEditText: EditText
    private lateinit var cpfEditText: EditText
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
        cpfEditText = findViewById(R.id.cpfInput)
        nameUserEditText = findViewById(R.id.nameUser)
        signUpButton = findViewById(R.id.signupButton)
        loginTextView = findViewById(R.id.loginTextView)

        applyCpfMask()

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val nameUser = nameUserEditText.text.toString().trim()
            val cpf = cpfEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && nameUser.isNotEmpty() && cpf.isNotEmpty()) {
                if (password == confirmPassword) {
                    // Verificar o Android ID antes de criar o usuário
                    val androidId = Secure.getString(contentResolver, Secure.ANDROID_ID)
                    checkIfAndroidIdExists(androidId) { exists ->
                        if (exists) {
                            Toast.makeText(this, "Este dispositivo já está registrado.", Toast.LENGTH_SHORT).show()
                        } else {
                            createBusinessUser(email, password, nameUser, cpf, androidId)
                        }
                    }
                } else {
                    Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        loginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun applyCpfMask() {
        cpfEditText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "###.###.###-##"
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                var str = s.toString().replace(Regex("[^\\d]"), "")
                val maskedStr = StringBuilder()
                var index = 0
                for (m in mask.toCharArray()) {
                    if (m != '#' && index < str.length) {
                        maskedStr.append(m)
                        continue
                    }
                    if (index >= str.length) break
                    maskedStr.append(str[index])
                    index++
                }

                isUpdating = true
                cpfEditText.setText(maskedStr.toString())
                cpfEditText.setSelection(maskedStr.length)
            }
        })
    }

    // Função para criar o usuário de negócio no Firebase Authentication
    private fun createBusinessUser(email: String, password: String, nameUser: String, cpf: String, androidId: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    createBusinessAndSubscription(email, nameUser, cpf, androidId)
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthWeakPasswordException) {
                        // Mensagem específica para senha fraca
                        Toast.makeText(this, "A senha é muito fraca. Por favor, escolha uma senha mais forte.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Mensagem genérica para outros erros
                        Toast.makeText(this, "Falha ao cadastrar. Tente novamente.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun createBusinessAndSubscription(email: String, nameUser: String, cpf: String, androidId: String) {
        val userId = auth.currentUser?.uid ?: return

        // Criar assinatura inicial sem verificação prévia
        createInitialSubscription(userId, cpf)

        // Criar dados do negócio com o Android ID
        createBusinessData(email, nameUser, cpf, androidId)
    }


    // Salva o token FCM para o usuário de negócio
    private fun saveFCMToken(businessId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                db.collection("business").document(businessId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "Token salvo com sucesso para $businessId.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "Erro ao salvar token: ${e.message}")
                    }
            } else {
                Log.e("FCM", "Erro ao obter token", task.exception)
            }
        }
    }

    // Cria o documento inicial do negócio e assinatura
    private fun createBusinessData(email: String, nameUser: String, cpf: String, androidId: String) {
        val businessId = auth.currentUser?.uid

        if (businessId != null) {
            val businessData = hashMapOf(
                "email" to email,
                "cpf" to cpf,
                "ownerName" to nameUser,
                "isActive" to true,
                "type" to "business",
                "ownerId" to businessId,
                "averageRating" to 0.0,
                "ratingCount" to 0,
                "androidId" to androidId // Adiciona o Android ID aqui
            )

            Log.d("BusinessData", "Dados do negócio: $businessData")

            saveFCMToken(businessId)

            db.collection("business").document(businessId)
                .set(businessData)
                .addOnSuccessListener {
                    Log.d("CreateBusinessData", "Dados do negócio salvos com sucesso: $businessData")
                    Toast.makeText(this, "Cadastro de negócio bem-sucedido!", Toast.LENGTH_SHORT).show()

                    // Redireciona para a tela de carregamento
                    val intent = Intent(this, RegisterBusinessActivity::class.java)
                    intent.putExtra("EMAIL_KEY", email) // Passando o email para a próxima Activity
                    startActivity(intent)
                    finish() // Finaliza a atividade atual
                }
                .addOnFailureListener { e ->
                    Log.e("CreateBusinessData", "Erro ao salvar dados do negócio: ${e.message}")
                    Toast.makeText(this, "Falha ao salvar dados do negócio.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Erro: UID do usuário não encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    // Cria o documento inicial da assinatura com 1 mês gratuito
    private fun createInitialSubscription(businessId: String, cpf: String) {
        val startDate = Timestamp.now()
        val calendar = Calendar.getInstance().apply {
            time = startDate.toDate()
            add(Calendar.MONTH, 1)  // Adiciona 1 mês ao início do período gratuito
        }
        val endDate = Timestamp(calendar.time)

        val subscriptionData = hashMapOf(
            "ownerId" to businessId,
            "cpf" to cpf,
            "startDate" to startDate,
            "endDate" to endDate,
            "isTrialActive" to true,
            "isActive" to false,
            "plan" to "trial"
        )

        db.collection("subscriptions").document(businessId)
            .set(subscriptionData)
            .addOnSuccessListener {
                Log.d("Subscription", "Assinatura inicial criada com sucesso.")
            }
            .addOnFailureListener { e ->
                Log.e("Subscription", "Erro ao criar assinatura: ${e.message}")
            }
    }

    private fun checkIfAndroidIdExists(androidId: String, callback: (Boolean) -> Unit) {
        db.collection("business")
            .whereEqualTo("androidId", androidId)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    callback(true) // Android ID já existe
                } else {
                    callback(false) // Android ID não encontrado, pode registrar
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Erro ao verificar Android ID: ${e.message}")
                callback(false)
            }
    }
}
