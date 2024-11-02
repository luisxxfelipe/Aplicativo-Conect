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
                    createBusinessUser(email, password, nameUser, cpf)
                } else {
                    Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT)
                    .show()
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
    private fun createBusinessUser(email: String, password: String, nameUser: String, cpf: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Se o usuário foi criado, ele agora está autenticado
                    checkCpfAndCreateBusiness(email, nameUser, cpf)
                } else {
                    Log.e("CreateBusinessUser", "Erro ao criar usuário: ${task.exception?.message}")
                    Toast.makeText(this, "Falha ao cadastrar. Tente novamente.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    // Verifica se o CPF já existe e, se não, cria o negócio
    private fun checkCpfAndCreateBusiness(email: String, nameUser: String, cpf: String) {
        db.collection("subscriptions")
            .whereEqualTo("cpf", cpf)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // CPF não encontrado, pode criar o negócio
                    createBusinessData(email, nameUser, cpf)
                } else {
                    Toast.makeText(
                        this,
                        "Este CPF já foi utilizado para uma assinatura.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("SubscriptionCheck", "Erro ao verificar CPF: ${e.message}")
            }
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
    private fun createBusinessData(email: String, nameUser: String, cpf: String) {
        val businessId = auth.currentUser?.uid

        if (businessId != null) {
            val businessData = hashMapOf(
                "email" to email,
                "cpf" to cpf,
                "ownerName" to nameUser,
                "isActive" to true,
                "type" to "business",
                "ownerId" to businessId
            )

            saveFCMToken(businessId)

            db.collection("business").document(businessId)
                .set(businessData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cadastro de negócio bem-sucedido!", Toast.LENGTH_SHORT)
                        .show()
                    createInitialSubscription(businessId, cpf)

                    // Redireciona para a tela de registro de mais dados
                    val intent = Intent(this, RegisterBusinessActivity::class.java)
                    intent.putExtra("EMAIL_KEY", email)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("CreateBusinessData", "Erro ao salvar dados do negócio: ${e.message}")
                    Toast.makeText(this, "Falha ao salvar dados do negócio.", Toast.LENGTH_SHORT)
                        .show()
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
}
