package com.conect.aplicativoconect.view.data

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R

class PaymentResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_result)

        val data: Uri? = intent?.data
        if (data != null) {
            when (data.host) {
                "success" -> handleSuccess()
                "failure" -> handleFailure()
                "pending" -> handlePending()
                else -> Log.e("PaymentResult", "Unknown payment result: ${data.host}")
            }
        } else {
            Log.e("PaymentResult", "No data received in intent")
        }
    }

    private fun handleSuccess() {
        Log.i("PaymentResult", "Payment successful")
        // Redirecionar ou mostrar mensagem
    }

    private fun handleFailure() {
        Log.e("PaymentResult", "Payment failed")
        // Redirecionar ou mostrar mensagem de erro
    }

    private fun handlePending() {
        Log.i("PaymentResult", "Payment pending")
        // Redirecionar ou mostrar mensagem de status pendente
    }
}
