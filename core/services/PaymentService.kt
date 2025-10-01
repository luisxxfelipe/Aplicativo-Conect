package com.conect.aplicativoconect.core.services

import android.content.Context
import com.conect.aplicativoconect.utils.Validator
import com.mercadopago.MercadoPago
import com.mercadopago.android.px.configuration.PaymentConfiguration
import com.mercadopago.android.px.core.MercadoPagoCheckout
import com.mercadopago.android.px.model.Payment
import com.mercadopago.android.px.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PaymentService(private val context: Context) {
    companion object {
        private const val PUBLIC_KEY = "APP_USR-b0edff4f-16e4-4de2-b50f-cab9e62c690c"
        private const val ACCESS_TOKEN = "APP_USR-4420457494967421-103123-3808288c4ac3409dcec40d2de9aec76a-474980834"
    }

    init {
        MercadoPago.init(context, PUBLIC_KEY)  // Init SDK v8 moderno
    }

    fun createPayment(
        amount: Double,
        title: String,
        email: String,
        onSuccess: (paymentId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Validator.isValidEmail(email) || amount <= 0) {
            onError("Dados inválidos (email ou amount)")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    // Config payment moderno v8
                    val paymentConfig = PaymentConfiguration(
                        paymentMethod = PaymentMethod.PIX,  // Prioriza Pix (prático BR)
                        paymentPreference = null  // Simples, sem preference complexa
                    )

                    // Abre checkout tela (funcional, nativa)
                    val checkout = MercadoPagoCheckout.Builder(
                        amount,  // Amount prático
                        ACCESS_TOKEN
                    ).build()

                    // Inicia payment (coroutines compatível)
                    val paymentResult = checkout.startPayment(context, paymentConfig)
                    if (paymentResult.status == Payment.Status.APPROVED) {
                        onSuccess(paymentResult.id)  // Success: paymentId para renew
                    } else {
                        onError("Pagamento falhou: ${paymentResult.status}")
                    }
                }
            } catch (e: Exception) {
                onError("Erro no pagamento: ${e.message}")
            }
        }
    }
}
