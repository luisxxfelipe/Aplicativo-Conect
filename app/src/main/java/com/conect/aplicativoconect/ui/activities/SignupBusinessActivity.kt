package com.conect.aplicativoconect.ui.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.Secure
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.utils.TokenManager
import com.conect.aplicativoconect.utils.Validator
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.conect.aplicativoconect.data.repositories.BusinessValidationRepository
import com.conect.aplicativoconect.data.models.BusinessValidationStatus
import kotlinx.coroutines.launch
import android.os.Build
import kotlinx.coroutines.launch
import java.util.Calendar


class SignupBusinessActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var businessValidationRepository: BusinessValidationRepository
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var nameUserEditText: EditText
    private lateinit var cpfEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var loginTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
    // Preencher campos com dados do Google se vierem via Intent
    val googleName = intent.getStringExtra("GOOGLE_NAME") ?: ""
    val googleEmail = intent.getStringExtra("GOOGLE_EMAIL") ?: ""
    if (googleName.isNotEmpty()) nameUserEditText.setText(googleName)
        if (googleEmail.isNotEmpty()) {
            emailEditText.setText(googleEmail)
            emailEditText.isEnabled = false
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_business)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        businessValidationRepository = BusinessValidationRepository()

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

            if (nameUser.isEmpty()) {
                nameUserEditText.error = "Nome obrigatório"
                return@setOnClickListener
            }
            if (!Validator.isValidEmail(email)) {
                emailEditText.error = "Email inválido"
                return@setOnClickListener
            }
            if (!Validator.isValidCPF(cpf)) {
                cpfEditText.error = "CPF inválido"
                return@setOnClickListener
            }

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && nameUser.isNotEmpty() && cpf.isNotEmpty()) {
                if (password == confirmPassword) {
                    // VALIDAÇÃO ANTI-FRAUD ROBUSTA
                    val androidId = Secure.getString(contentResolver, Secure.ANDROID_ID)
                    performBusinessValidation(email, password, nameUser, cpf, androidId)
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

    fun isValidCpf(cpf: String): Boolean {
        // Remover a máscara
        val cleanCpf = cpf.replace(Regex("[^\\d]"), "")

        // Verificar se o CPF tem 11 dígitos
        if (cleanCpf.length != 11 || cleanCpf.all { it == cleanCpf[0] }) return false

        // Validar primeiro dígito verificador
        var sum = 0
        for (i in 0 until 9) {
            sum += cleanCpf[i].digitToInt() * (10 - i)
        }
        var firstCheck = 11 - (sum % 11)
        if (firstCheck == 10 || firstCheck == 11) firstCheck = 0
        if (cleanCpf[9].digitToInt() != firstCheck) return false

        // Validar segundo dígito verificador
        sum = 0
        for (i in 0 until 10) {
            sum += cleanCpf[i].digitToInt() * (11 - i)
        }
        var secondCheck = 11 - (sum % 11)
        if (secondCheck == 10 || secondCheck == 11) secondCheck = 0
        if (cleanCpf[10].digitToInt() != secondCheck) return false

        return true
    }


    private fun applyCpfMask() {
        cpfEditText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "###.###.###-##"

            override fun afterTextChanged(s: Editable?) {
                // Após a alteração do texto, você pode verificar se o CPF é válido
                val cpf = cpfEditText.text.toString()
                if (isValidCpf(cpf)) {
                    cpfEditText.error = null  // Remove erro se o CPF for válido
                } else {
                    cpfEditText.error = "CPF inválido"  // Exibe erro se o CPF for inválido
                }
            }

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
    private fun createBusinessUser(
        email: String,
        password: String,
        nameUser: String,
        cpf: String,
        androidId: String,
        validationResult: com.conect.aplicativoconect.data.repositories.BusinessValidationResult
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    createBusinessAndSubscription(email, nameUser, cpf, androidId, validationResult)
                } else {
                    val exception = task.exception
                    when {
                        exception is FirebaseAuthWeakPasswordException -> {
                            Toast.makeText(
                                this,
                                "A senha é muito fraca. Por favor, escolha uma senha mais forte.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        exception?.message?.contains("email address is already in use") == true -> {
                            Toast.makeText(
                                this,
                                "Este email já está cadastrado. Tente fazer login ou use outro email.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        exception?.message?.contains("invalid email") == true -> {
                            Toast.makeText(
                                this,
                                "Email inválido. Verifique o formato do email.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                this,
                                "Erro ao cadastrar empresa: ${exception?.message ?: "Tente novamente"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
    }

    private fun createBusinessAndSubscription(
        email: String,
        nameUser: String,
        cpf: String,
        androidId: String,
        validationResult: com.conect.aplicativoconect.data.repositories.BusinessValidationResult
    ) {
        val userId = auth.currentUser?.uid ?: return

        // Criar assinatura inicial sem verificação prévia
        createInitialSubscription(userId, cpf)

        // Criar dados do negócio com o Android ID
        createBusinessData(email, nameUser, cpf, androidId, validationResult)
    }


    // Cria o documento inicial do negócio e assinatura
    private fun createBusinessData(
        email: String,
        nameUser: String,
        cpf: String,
        androidId: String,
        validationResult: com.conect.aplicativoconect.data.repositories.BusinessValidationResult
    ) {
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

            db.collection("business").document(businessId)
                .set(businessData)
                .addOnSuccessListener {
                    Log.d(
                        "CreateBusinessData",
                        "Dados do negócio salvos com sucesso: $businessData"
                    )
                    
                    // SALVAR RESULTADO DA VALIDAÇÃO APÓS AUTENTICAÇÃO
                    validationResult.validationData?.let { validationData ->
                        lifecycleScope.launch {
                            try {
                                businessValidationRepository.saveValidationResult(validationData)
                                Log.d("BusinessValidation", "Resultado da validação salvo com sucesso")
                            } catch (e: Exception) {
                                Log.e("BusinessValidation", "Erro ao salvar resultado da validação: ${e.message}")
                            }
                        }
                    }
                    
                    Toast.makeText(this, "Cadastro de negócio bem-sucedido!", Toast.LENGTH_SHORT)
                        .show()

                    // Redireciona para a tela de carregamento
                    val intent = Intent(this, RegisterBusinessActivity::class.java)
                    intent.putExtra("EMAIL_KEY", email) // Passando o email para a próxima Activity
                    startActivity(intent)
                    finish() // Finaliza a atividade atual
                    lifecycleScope.launch {
                        TokenManager.saveTokenForBusiness(businessId)
                    }
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
    
    // ===== SISTEMA ANTI-FRAUD ROBUSTO =====
    
    /**
     * VALIDAÇÃO COMPLETA ANTI-FRAUD
     */
    private fun performBusinessValidation(
        email: String,
        password: String, 
        nameUser: String,
        cpf: String,
        androidId: String
    ) {
        lifecycleScope.launch {
            try {
                // COLETA DE DADOS PARA VALIDAÇÃO
                val deviceInfo = getDeviceInfo()
                val ipAddress = getDeviceIP()
                
                // EXECUTAR VALIDAÇÃO ANTI-FRAUD
                val validationResult = businessValidationRepository.validateBusinessRegistration(
                    cpf = cpf,
                    phone = "", // Será coletado posteriormente
                    address = "", // Será coletado posteriormente
                    deviceInfo = deviceInfo,
                    androidId = androidId,
                    ipAddress = ipAddress,
                    latitude = 0.0, // Será coletado posteriormente
                    longitude = 0.0 // Será coletado posteriormente
                )
                
                // PROCESSAR RESULTADO DA VALIDAÇÃO
                when (validationResult.validationStatus) {
                    BusinessValidationStatus.APPROVED -> {
                        // APROVADO - Prosseguir com cadastro
                        createBusinessUser(email, password, nameUser, cpf, androidId, validationResult)
                    }
                    
                    BusinessValidationStatus.UNDER_REVIEW -> {
                        // REVISÃO MANUAL - Bloquear temporariamente
                        showValidationDialog(
                            "Cadastro em Análise",
                            "Seu cadastro será analisado por nossa equipe. " +
                            "Você receberá um email em até 24 horas com o resultado.\n\n" +
                            "Motivo: Verificação de segurança preventiva.\n" +
                            "Score de segurança: ${validationResult.fraudScore}/100"
                        )
                    }
                    
                    BusinessValidationStatus.ADDITIONAL_INFO -> {
                        // 📋 DOCUMENTOS NECESSÁRIOS
                        showValidationDialog(
                            "Documentos Necessários",
                            "Para finalizar seu cadastro, será necessário enviar documentos adicionais:\n\n" +
                            "• Documento de identidade\n" +
                            "• Comprovante de endereço\n" +
                            "• Licença comercial (se aplicável)\n\n" +
                            "Score de segurança: ${validationResult.fraudScore}/100"
                        )
                    }
                    
                    BusinessValidationStatus.FRAUD_DETECTED -> {
                        // 🚫 FRAUDE DETECTADA - Bloquear permanentemente
                        showValidationDialog(
                            "Cadastro Bloqueado",
                            "Não foi possível realizar o cadastro devido a inconsistências detectadas.\n\n" +
                            "Se você acredita que isso é um erro, entre em contato conosco.\n\n" +
                            "Motivos: ${validationResult.fraudFlags.joinToString(", ")}"
                        )
                    }
                    
                    BusinessValidationStatus.REJECTED -> {
                        // ❌ REJEITADO
                        showValidationDialog(
                            "Cadastro Rejeitado",
                            "Não foi possível validar os dados fornecidos. " +
                            "Verifique as informações e tente novamente.\n\n" +
                            "${validationResult.errorMessage ?: "Erro desconhecido"}"
                        )
                    }
                    
                    else -> {
                        // Estado não tratado
                        createBusinessUser(email, password, nameUser, cpf, androidId, validationResult)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("BusinessValidation", "Erro na validação anti-fraud: ${e.message}")
                
                // Em caso de erro, usar validação básica como fallback
                val fallbackValidationResult = com.conect.aplicativoconect.data.repositories.BusinessValidationResult(
                    isValid = true,
                    validationStatus = com.conect.aplicativoconect.data.models.BusinessValidationStatus.APPROVED,
                    fraudScore = 0,
                    errorMessage = "Validação fallback devido a erro: ${e.message}"
                )
                checkIfAndroidIdExists(androidId) { exists ->
                    if (exists) {
                        Toast.makeText(
                            this@SignupBusinessActivity,
                            "Este dispositivo já está registrado.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        createBusinessUser(email, password, nameUser, cpf, androidId, fallbackValidationResult)
                    }
                }
            }
        }
    }
    
    /**
     * COLETA INFORMAÇÕES DO DISPOSITIVO PARA FINGERPRINTING
     */
    private fun getDeviceInfo(): String {
        return buildString {
            append("Model:${Build.MODEL};")
            append("Brand:${Build.BRAND};")
            append("SDK:${Build.VERSION.SDK_INT};")
            append("Release:${Build.VERSION.RELEASE};")
            append("Manufacturer:${Build.MANUFACTURER};")
            append("Hardware:${Build.HARDWARE};")
            append("Display:${Build.DISPLAY};")
        }
    }
    
    /**
     * OBTÉM IP DO DISPOSITIVO (APROXIMADO)
     */
    private fun getDeviceIP(): String {
        return try {
            // Implementação básica - pode ser melhorada com API externa
            "192.168.1.100" // Placeholder
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * DIALOG PARA MOSTRAR RESULTADOS DA VALIDAÇÃO
     */
    private fun showValidationDialog(title: String, message: String) {
        runOnUiThread {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Entendi") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }
}
