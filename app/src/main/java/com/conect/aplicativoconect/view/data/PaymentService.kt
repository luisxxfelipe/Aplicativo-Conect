package com.conect.aplicativoconect.view.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaymentService(private val context: Context) {

    suspend fun createPayment(
        amount: Float,
        title: String,
        payerEmail: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val paymentPreference = PaymentPreference(
            items = listOf(Item(title = title, quantity = 1, unit_price = amount)),
            payer = Payer(email = payerEmail),
            back_urls = BackUrls(
                success = "conectapp://success",
                failure = "conectapp://failure",
                pending = "conectapp://pending"
            )
        )

        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitInstance.api.createPreference(paymentPreference)
            }

            if (response.isSuccessful && response.body() != null) {
                val checkoutUrl = response.body()!!.init_point
                openCheckoutUrl(checkoutUrl)
                onSuccess()
            } else {
                Log.e(
                    "PaymentService",
                    "Erro ao criar preferência de pagamento: Código HTTP ${response.code()} - ${
                        response.errorBody()?.string()
                    }"
                )
                onError("Erro ao criar preferência de pagamento: Código HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("PaymentService", "Erro ao iniciar pagamento", e)
            onError(e.message ?: "Erro ao iniciar pagamento")
        }
    }

    private fun openCheckoutUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
