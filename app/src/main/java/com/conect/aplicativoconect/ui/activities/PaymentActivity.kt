package com.conect.aplicativoconect.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.core.services.PaymentService
import com.conect.aplicativoconect.databinding.ActivityPaymentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class PaymentActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var paymentService: PaymentService
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeComponents()
        setupUI()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Processar deep link se tiver dados ou se a action for VIEW
        if (intent?.data != null || intent?.action == Intent.ACTION_VIEW) {
            handleDeepLink(intent)
        }
    }
    
    private fun initializeComponents() {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        paymentService = PaymentService(this)
    }
    
    private fun setupUI() {
        // Configurar título
        binding.textTitle.text = "Escolha seu Plano"
        binding.textSubtitle.text = "Desbloqueie todos os recursos do ConecteX"
        
        // Configurar botões
        binding.buttonMonthlyPlan.setOnClickListener {
            startMonthlyPayment()
        }
        
        binding.buttonAnnualPlan.setOnClickListener {
            startAnnualPayment()
        }
        
        binding.buttonBack.setOnClickListener {
            finish()
        }
        
        // Mostrar preços
        binding.textMonthlyPrice.text = "R$ ${PaymentService.MONTHLY_PLAN_PRICE}/mês"
        binding.textAnnualPrice.text = "R$ ${PaymentService.ANNUAL_PLAN_PRICE}/ano"
        binding.textAnnualSaving.text = "Economize R$ ${(PaymentService.MONTHLY_PLAN_PRICE * 12 - PaymentService.ANNUAL_PLAN_PRICE).toInt()}"
    }
    

    
    private fun startMonthlyPayment() {
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            binding.buttonMonthlyPlan.isEnabled = false
            binding.buttonMonthlyPlan.text = "Processando..."
            
            paymentService.startMonthlySubscriptionCheckout(
                activity = this,
                userEmail = userEmail
            ) { error ->
                showError(error)
                resetButton(binding.buttonMonthlyPlan, "Plano Mensal - R$ ${PaymentService.MONTHLY_PLAN_PRICE}")
            }
        } else {
            showError("Usuário não autenticado")
        }
    }
    
    private fun startAnnualPayment() {
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            binding.buttonAnnualPlan.isEnabled = false
            binding.buttonAnnualPlan.text = "Processando..."
            
            paymentService.startAnnualSubscriptionCheckout(
                activity = this,
                userEmail = userEmail
            ) { error ->
                showError(error)
                resetButton(binding.buttonAnnualPlan, "Plano Anual - R$ ${PaymentService.ANNUAL_PLAN_PRICE}")
            }
        } else {
            showError("Usuário não autenticado")
        }
    }
    
    private fun handleDeepLink(intent: Intent?) {
        paymentService.handleDeepLinkResult(
            intent = intent,
            onSuccess = { paymentId, status ->
                
                when (status) {
                    "approved" -> {
                        if (paymentId == null) {
                            showError("Pagamento aprovado, mas sem ID. Tente atualizar mais tarde.")
                        } else {
                            updateSubscriptionStatus(paymentId, "active") { success, errorMsg ->
                                if (success) {
                                    showSuccess("Pagamento aprovado! Redirecionando...")
                                    navigateToHome()
                                } else {
                                    showError("Pagamento ok, mas falhou ao salvar assinatura: $errorMsg")
                                }
                            }
                        }
                    }
                    "pending" -> {
                        if (paymentId == null) {
                            showError("Pagamento pendente sem ID. Tente novamente.")
                        } else {
                            updateSubscriptionStatus(paymentId, "pending") { success, errorMsg ->
                                if (success) {
                                    showSuccess("Pagamento pendente. Você será notificado quando for aprovado.")
                                    navigateToHome()
                                } else {
                                    showError("Falhou ao salvar status pendente: $errorMsg")
                                }
                            }
                        }
                    }
                    else -> {
                        showError("Status de pagamento desconhecido: $status")
                        resetButtons()
                    }
                }
            },
            onError = { error ->
                showError("Erro no pagamento: $error")
                resetButtons()
            }
        )
    }
    
    private fun updateSubscriptionStatus(paymentId: String, status: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onComplete(false, "Usuário não autenticado")
            return
        }
        
        // Determinar se é plano anual baseado no paymentId ou contexto
        val isAnnual = paymentId.contains("annual") || 
                      binding.buttonAnnualPlan.text.toString().contains("Processando")
        
        val subscriptionData = hashMapOf(
            "ownerId" to userId,
            "status" to status,
            "paymentId" to paymentId,
            "planType" to if (isAnnual) "annual" else "monthly",
            "startDate" to Date(),
            "endDate" to getEndDate(isAnnual),
            "updatedAt" to Date()
        )
        
        firestore.collection("subscriptions")
            .document(userId)
            .set(subscriptionData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
            }
    }
    
    private fun getEndDate(isAnnual: Boolean = false): Date {
        val calendar = Calendar.getInstance()
        if (isAnnual) {
            calendar.add(Calendar.YEAR, 1)
        } else {
            calendar.add(Calendar.MONTH, 1)
        }
        return calendar.time
    }
    
    private fun navigateToHome() {
        // Determinar qual home abrir baseado no tipo de usuário
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("business").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // É um business user
                    startActivity(Intent(this, AdminHomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                } else {
                    // É um cliente
                    startActivity(Intent(this, ClienteHomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                finish()
            }
            .addOnFailureListener {
                // Default para AdminHome se houver erro
                startActivity(Intent(this, AdminHomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun resetButton(button: android.widget.Button, originalText: String) {
        button.isEnabled = true
        button.text = originalText
    }
    
    private fun resetButtons() {
        resetButton(binding.buttonMonthlyPlan, "Plano Mensal - R$ ${PaymentService.MONTHLY_PLAN_PRICE}")
        resetButton(binding.buttonAnnualPlan, "Plano Anual - R$ ${PaymentService.ANNUAL_PLAN_PRICE}")
    }
    

}