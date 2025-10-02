package com.conect.aplicativoconect.core.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.conect.aplicativoconect.utils.Validator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * Serviço de pagamentos para assinaturas
 * Preparado para integração com gateways de pagamento
 */
class PaymentService(private val context: Context) {
    companion object {
        // Planos de assinatura
        const val MONTHLY_PLAN_PRICE = 29.90
        const val ANNUAL_PLAN_PRICE = 299.00
        
        const val MONTHLY_PLAN_ID = "monthly_subscription"
        const val ANNUAL_PLAN_ID = "annual_subscription"
        
        // Códigos de resposta para pagamento
        const val PAYMENT_SUCCESS = 100
        const val PAYMENT_PENDING = 200
        const val PAYMENT_CANCELLED = 300
        const val PAYMENT_ERROR = 400
    }

    /**
     * Inicia o checkout para assinatura mensal
     */
    fun startMonthlySubscriptionCheckout(
        userEmail: String,
        launcher: ActivityResultLauncher<Intent>,
        onError: (String) -> Unit
    ) {
        startSubscriptionCheckout(
            planId = MONTHLY_PLAN_ID,
            amount = MONTHLY_PLAN_PRICE,
            title = "ConecteX - Plano Mensal",
            description = "Assinatura mensal do ConecteX com todos os recursos",
            userEmail = userEmail,
            launcher = launcher,
            onError = onError
        )
    }
    
    /**
     * Inicia o checkout para assinatura anual
     */
    fun startAnnualSubscriptionCheckout(
        userEmail: String,
        launcher: ActivityResultLauncher<Intent>,
        onError: (String) -> Unit
    ) {
        startSubscriptionCheckout(
            planId = ANNUAL_PLAN_ID,
            amount = ANNUAL_PLAN_PRICE,
            title = "ConecteX - Plano Anual",
            description = "Assinatura anual do ConecteX com 2 meses grátis",
            userEmail = userEmail,
            launcher = launcher,
            onError = onError
        )
    }
    
    /**
     * Método interno para configurar e iniciar o checkout
     */
    private fun startSubscriptionCheckout(
        planId: String,
        amount: Double,
        title: String,
        description: String,
        userEmail: String,
        launcher: ActivityResultLauncher<Intent>,
        onError: (String) -> Unit
    ) {
        if (!Validator.isValidEmail(userEmail) || amount <= 0) {
            onError("Dados inválidos (email ou valor)")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Aqui seria integrado com um gateway de pagamento real
                // Por exemplo: PagSeguro, PayPal, Stripe, etc.
                
                // Simular dados do checkout
                val checkoutData = createPaymentIntent(planId, amount, title, description, userEmail)
                
                withContext(Dispatchers.Main) {
                    launcher.launch(checkoutData)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Erro ao configurar pagamento: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Cria um Intent simulado para pagamento
     * Em produção, seria substituído pela integração real
     */
    private fun createPaymentIntent(
        planId: String,
        amount: Double,
        title: String,
        description: String,
        userEmail: String
    ): Intent {
        return Intent().apply {
            putExtra("plan_id", planId)
            putExtra("amount", amount)
            putExtra("title", title)
            putExtra("description", description)
            putExtra("user_email", userEmail)
            putExtra("timestamp", System.currentTimeMillis())
        }
    }
    
    /**
     * Processa o resultado do pagamento
     */
    fun handlePaymentResult(
        resultCode: Int,
        data: Intent?,
        onSuccess: (paymentId: String, status: String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val paymentId = data?.getStringExtra("payment_id") 
                        ?: "payment_${System.currentTimeMillis()}"
                    val status = data?.getStringExtra("status") ?: "approved"
                    onSuccess(paymentId, status)
                    android.util.Log.d("PaymentService", "Pagamento processado com sucesso: $paymentId")
                }
                Activity.RESULT_CANCELED -> {
                    onError("Pagamento cancelado pelo usuário")
                    android.util.Log.d("PaymentService", "Pagamento cancelado")
                }
                PAYMENT_PENDING -> {
                    val paymentId = data?.getStringExtra("payment_id") 
                        ?: "pending_${System.currentTimeMillis()}"
                    onSuccess(paymentId, "pending")
                    android.util.Log.d("PaymentService", "Pagamento pendente: $paymentId")
                }
                else -> {
                    onError("Pagamento rejeitado ou falhou")
                    android.util.Log.d("PaymentService", "Pagamento falhou com código: $resultCode")
                }
            }
        } catch (e: Exception) {
            onError("Erro ao processar resultado: ${e.message}")
            android.util.Log.e("PaymentService", "Erro no processamento: ${e.message}")
        }
    }
    
    /**
     * Valida se o pagamento está ativo
     */
    fun validateSubscription(
        paymentId: String,
        onResult: (isValid: Boolean, expiryDate: Long?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Aqui seria consultado o status real do pagamento
                // Por enquanto simula validação
                val isValid = paymentId.isNotEmpty()
                val expiryDate = if (isValid) {
                    System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 dias
                } else null
                
                withContext(Dispatchers.Main) {
                    onResult(isValid, expiryDate)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, null)
                }
            }
        }
    }
}
