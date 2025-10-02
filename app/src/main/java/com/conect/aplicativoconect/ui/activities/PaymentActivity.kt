package com.conect.aplicativoconect.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    
    // ActivityResultLauncher para o checkout de pagamento
    private val checkoutLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handlePaymentResult(result.resultCode, result.data)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeComponents()
        setupUI()
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
                userEmail = userEmail,
                launcher = checkoutLauncher
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
                userEmail = userEmail,
                launcher = checkoutLauncher
            ) { error ->
                showError(error)
                resetButton(binding.buttonAnnualPlan, "Plano Anual - R$ ${PaymentService.ANNUAL_PLAN_PRICE}")
            }
        } else {
            showError("Usuário não autenticado")
        }
    }
    
    private fun handlePaymentResult(resultCode: Int, data: Intent?) {
        paymentService.handlePaymentResult(
            resultCode = resultCode,
            data = data,
            onSuccess = { paymentId, status ->
                when (status) {
                    "approved" -> {
                        updateSubscriptionStatus(paymentId, "active")
                        showSuccess("Pagamento aprovado! Redirecionando...")
                        navigateToHome()
                    }
                    "pending" -> {
                        updateSubscriptionStatus(paymentId, "pending")
                        showSuccess("Pagamento pendente. Você será notificado quando for aprovado.")
                        navigateToHome()
                    }
                }
            },
            onError = { error ->
                showError(error)
                resetButtons()
            }
        )
    }
    
    private fun updateSubscriptionStatus(paymentId: String, status: String) {
        val userId = auth.currentUser?.uid ?: return
        
        val subscriptionData = hashMapOf(
            "status" to status,
            "paymentId" to paymentId,
            "startDate" to Date(),
            "endDate" to getEndDate(),
            "updatedAt" to Date()
        )
        
        firestore.collection("subscriptions")
            .document(userId)
            .set(subscriptionData)
            .addOnSuccessListener {
                Log.d("PaymentActivity", "Subscription updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("PaymentActivity", "Error updating subscription", e)
            }
    }
    
    private fun getEndDate(): Date {
        val calendar = Calendar.getInstance()
        // Por padrão, adiciona 1 mês. Ajustar conforme o plano escolhido
        calendar.add(Calendar.MONTH, 1)
        return calendar.time
    }
    
    private fun navigateToHome() {
        // Determinar qual home abrir baseado no tipo de usuário
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("business").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // É um business user
                    startActivity(Intent(this, AdminHomeActivity::class.java))
                } else {
                    // É um cliente
                    startActivity(Intent(this, ClienteHomeActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                // Default para AdminHome se houver erro
                startActivity(Intent(this, AdminHomeActivity::class.java))
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