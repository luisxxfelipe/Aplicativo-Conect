package com.conect.aplicativoconect.core.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.conect.aplicativoconect.utils.Validator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL


class PaymentService(private val context: Context) {
    companion object {
        // Planos de assinatura - Valores menores para teste
        const val MONTHLY_PLAN_PRICE = 27.00
        const val ANNUAL_PLAN_PRICE = 250.00
        
        const val MONTHLY_PLAN_ID = "monthly_subscription"
        const val ANNUAL_PLAN_ID = "annual_subscription"
        
        // CREDENCIAIS DE TESTE - Usando para testar primeiro
        //private const val PUBLIC_KEY = "TEST-9284fe3c-586d-4113-b2e5-2b4fe27a060d"
        //private const val ACCESS_TOKEN = "TEST-4420457494967421-101116-d76c7dd42ff4a52926452b24754ee40e-474980834"
        
        // CREDENCIAIS DE PRODUÇÃO - CHECKOUT PRO (comentadas até os testes passarem)
        private const val PUBLIC_KEY = "APP_USR-52048a09-ec7f-4ef5-8289-45d3426aa42e"
        private const val ACCESS_TOKEN = "APP_USR-4420457494967421-101116-2bb6acd2b1e0e58d6808c00eb816735b-474980834"
        
        
        // URLs do Mercado Pago - CHECKOUT PRO
        private const val MP_API_BASE = "https://api.mercadopago.com"
        private const val MP_PREFERENCES_URL = "$MP_API_BASE/checkout/preferences"
        private const val MP_PAYMENTS_URL = "$MP_API_BASE/v1/payments"
        
        // Códigos de resposta do Mercado Pago
        const val PAYMENT_SUCCESS = 100
        const val PAYMENT_PENDING = 200
        const val PAYMENT_CANCELLED = 300
        const val PAYMENT_ERROR = 400
    }

    /**
     * Inicia o checkout para assinatura mensal
     */
    fun startMonthlySubscriptionCheckout(
        activity: Activity,
        userEmail: String,
        onError: (String) -> Unit
    ) {
        startSubscriptionCheckout(
            activity = activity,
            planId = MONTHLY_PLAN_ID,
            amount = MONTHLY_PLAN_PRICE,
            title = "ConecteX - Plano Mensal",
            description = "Assinatura mensal do ConecteX com todos os recursos",
            userEmail = userEmail,
            onError = onError
        )
    }
    
    /**
     * Inicia o checkout para assinatura anual
     */
    fun startAnnualSubscriptionCheckout(
        activity: Activity,
        userEmail: String,
        onError: (String) -> Unit
    ) {
        startSubscriptionCheckout(
            activity = activity,
            planId = ANNUAL_PLAN_ID,
            amount = ANNUAL_PLAN_PRICE,
            title = "ConecteX - Plano Anual",
            description = "Assinatura anual do ConecteX com 2 meses grátis",
            userEmail = userEmail,
            onError = onError
        )
    }
    
    /**
     * Método interno para configurar e iniciar o checkout usando Custom Tabs
     */
    private fun startSubscriptionCheckout(
        activity: Activity,
        planId: String,
        amount: Double,
        title: String,
        description: String,
        userEmail: String,
        onError: (String) -> Unit
    ) {
        if (!Validator.isValidEmail(userEmail) || amount <= 0) {
            onError("Dados inválidos (email ou valor)")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Criar preferência real no Mercado Pago
                val initPoint = createMercadoPagoCheckout(
                    planId = planId,
                    amount = amount,
                    title = title,
                    description = description,
                    userEmail = userEmail
                )
                
                withContext(Dispatchers.Main) {
                    try {
                        // Abrir checkout via Custom Tabs (abordagem oficial)
                        val customTabsIntent = CustomTabsIntent.Builder().build()
                        customTabsIntent.launchUrl(activity, Uri.parse(initPoint))
                    } catch (e: Exception) {
                        onError("Erro ao abrir checkout: ${e.message}")
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Erro ao criar pagamento: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Cria preferência REAL do Checkout Pro e retorna o init_point
     */
    private suspend fun createMercadoPagoCheckout(
        planId: String,
        amount: Double,
        title: String,
        description: String,
        userEmail: String
    ): String {
        return createCheckoutProPreference(planId, amount, title, description, userEmail)
    }
    
    /**
     * Cria preferência do Checkout Pro no Mercado Pago
     */
    private suspend fun createCheckoutProPreference(
        planId: String,
        amount: Double,
        title: String,
        description: String,
        userEmail: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(MP_PREFERENCES_URL)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $ACCESS_TOKEN")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                // JSON para Checkout Pro
                val preferenceJson = JSONObject().apply {
                    // Items
                    put("items", org.json.JSONArray().put(
                        JSONObject().apply {
                            put("title", title)
                            put("description", description)
                            put("quantity", 1)
                            put("unit_price", amount)
                            put("currency_id", "BRL")
                        }
                    ))
                    
                    // Payer (removido o prefill para evitar bloqueios quando o e-mail é do mesmo dono da conta)
                    // O Checkout Pro solicitará o e-mail do pagador na própria página
                    
                    // Back URLs - usando domínio configurado no AndroidManifest
                    put("back_urls", JSONObject().apply {
                        put("success", "https://conectex-pages.vercel.app/success")
                        put("failure", "https://conectex-pages.vercel.app/failure")
                        put("pending", "https://conectex-pages.vercel.app/pending")
                    })
                    put("auto_return", "approved")
                    // Forçar estados finais (approved/rejected) e evitar intermediários
                    put("binary_mode", true)
                    
                    // Referência externa
                    put("external_reference", planId)
                    
                    // Configurações adicionais
                    put("statement_descriptor", "CONECTX")
                    // Opcional: webhook por preferência (se/quando houver servidor)
                    // put("notification_url", "https://seu-dominio.com/webhooks/mp")
                }
                
                // Enviar requisição
                val outputStream = connection.outputStream
                outputStream.write(preferenceJson.toString().toByteArray(Charsets.UTF_8))
                outputStream.flush()
                outputStream.close()
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val responseJson = JSONObject(response)
                    val initPoint = responseJson.getString("init_point")
                    
                    initPoint
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Erro desconhecido"
                    throw Exception("Erro ao criar checkout MP: $responseCode - $errorResponse")
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    /**
     * DEPRECATED - 1. Cria plano de assinatura no Mercado Pago
     */
    private suspend fun createSubscriptionPlan(
        planId: String,
        amount: Double,
        title: String,
        description: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$MP_API_BASE/preapproval_plan") // DEPRECATED
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $ACCESS_TOKEN")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                // JSON para criar plano de assinatura
                val planJson = JSONObject().apply {
                    put("reason", title)
                    put("auto_recurring", JSONObject().apply {
                        if (planId.contains("annual")) {
                            // Plano anual: cobrança anual
                            put("frequency", 1)
                            put("frequency_type", "years")
                        } else {
                            // Plano mensal: cobrança mensal  
                            put("frequency", 1)
                            put("frequency_type", "months")
                        }
                        put("billing_day_proportional", false)
                        put("transaction_amount", amount)
                        put("currency_id", "BRL")
                    })
                    put("back_url", "https://conectx.app/payment/success")
                }
                
                // Enviar requisição
                val outputStream = connection.outputStream
                outputStream.write(planJson.toString().toByteArray(Charsets.UTF_8))
                outputStream.flush()
                outputStream.close()
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val responseJson = JSONObject(response)
                    val planMpId = responseJson.getString("id")
                    
                    planMpId
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Erro desconhecido"
                    throw Exception("Erro ao criar plano MP: $responseCode - $errorResponse")
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    /**
     * DEPRECATED - 2. Cria assinatura do usuário no Mercado Pago
     */
    private suspend fun createUserSubscription(
        planMpId: String,
        userEmail: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$MP_API_BASE/preapproval") // DEPRECATED
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $ACCESS_TOKEN")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                // JSON para criar assinatura do usuário
                val subscriptionJson = JSONObject().apply {
                    put("preapproval_plan_id", planMpId)
                    put("payer_email", userEmail)
                    put("back_url", "https://conectx.app/payment/success")
                    put("status", "authorized")
                }
                
                // Enviar requisição
                val outputStream = connection.outputStream
                outputStream.write(subscriptionJson.toString().toByteArray(Charsets.UTF_8))
                outputStream.flush()
                outputStream.close()
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val responseJson = JSONObject(response)
                    val initPoint = responseJson.getString("init_point")
                    
                    initPoint
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Erro desconhecido"
                    throw Exception("Erro ao criar assinatura MP: $responseCode - $errorResponse")
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * DEPRECATED - Mantido para referência
     */
    private suspend fun createRealPreference(
        planId: String,
        amount: Double,
        title: String,
        description: String,
        userEmail: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(MP_PREFERENCES_URL)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $ACCESS_TOKEN")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                // JSON com configurações mais robustas
                // VERSÃO ULTRA SIMPLES PARA TESTE - Aplicação não homologada
                val preferenceJson = JSONObject().apply {
                    // ITEMS - Configuração mínima
                    put("items", org.json.JSONArray().put(
                        JSONObject().apply {
                            put("title", title)
                            put("quantity", 1)
                            put("unit_price", amount)
                        }
                    ))
                    
                    // PAYER - Apenas email
                    put("payer", JSONObject().apply {
                        put("email", userEmail)
                    })
                    
                    // BACK URLs para retorno
                    put("back_urls", JSONObject().apply {
                        put("success", "https://conectex-pages.vercel.app/success")
                        put("failure", "https://conectex-pages.vercel.app/failure") 
                        put("pending", "https://conectex-pages.vercel.app/pending")
                    })
                }
                
                // Enviar requisição
                val outputStream = connection.outputStream
                outputStream.write(preferenceJson.toString().toByteArray(Charsets.UTF_8))
                outputStream.flush()
                outputStream.close()
                
                // Processar resposta
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val responseJson = JSONObject(response)
                    val initPoint = responseJson.getString("init_point")
                    
                    initPoint
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Erro desconhecido"
                    
                    // Tratamento específico para erros comuns
                    val errorMessage = when (responseCode) {
                        400 -> "Dados inválidos na requisição. Verifique as credenciais e configurações."
                        401 -> "Credenciais inválidas. Verifique o ACCESS_TOKEN."
                        403 -> "Acesso negado. Verifique as permissões da aplicação."
                        404 -> "Endpoint não encontrado. Verifique a URL da API."
                        422 -> "Erro de validação. Verifique os dados da preferência."
                        500 -> "Erro interno do servidor Mercado Pago."
                        else -> "Erro HTTP $responseCode: $errorResponse"
                    }
                    
                    throw Exception(errorMessage)
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    /**
     * Processa resultado REAL do Deep Link do Mercado Pago
     */
    fun handleDeepLinkResult(
        intent: Intent?,
        onSuccess: (paymentId: String?, status: String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val uri = intent?.data
            
            if (uri != null) {
                val scheme = uri.scheme
                val host = uri.host
                val path = uri.path
                
                when {
                    // Scheme customizado: conectex://payment/*
                    scheme == "conectex" && host == "payment" -> {
                        when {
                            path?.contains("success") == true -> {
                                val paymentId = uri.getQueryParameter("payment_id")
                                val status = uri.getQueryParameter("status") ?: "approved"
                                val collectionId = uri.getQueryParameter("collection_id")
                                
                                onSuccess(paymentId ?: collectionId, status)
                            }
                            path?.contains("pending") == true -> {
                                val paymentId = uri.getQueryParameter("payment_id")
                                val collectionId = uri.getQueryParameter("collection_id")
                                
                                onSuccess(paymentId ?: collectionId, "pending")
                            }
                            path?.contains("failure") == true -> {
                                val errorCode = uri.getQueryParameter("error_code")
                                val errorMessage = uri.getQueryParameter("error_message")
                                
                                onError("Pagamento rejeitado: $errorCode - $errorMessage")
                            }
                            else -> {
                                onError("Status de pagamento desconhecido: $path")
                            }
                        }
                    }
                    // URLs HTTP: https://conectex-pages.vercel.app/*
                    scheme == "https" && host == "conectex-pages.vercel.app" -> {
                        when {
                            path == "/success" || path?.contains("success") == true -> {
                                val paymentId = uri.getQueryParameter("payment_id")
                                val status = uri.getQueryParameter("status") ?: "approved"
                                val collectionId = uri.getQueryParameter("collection_id")
                                
                                onSuccess(paymentId ?: collectionId, status)
                            }
                            path == "/pending" || path?.contains("pending") == true -> {
                                val paymentId = uri.getQueryParameter("payment_id")
                                val collectionId = uri.getQueryParameter("collection_id")
                                
                                onSuccess(paymentId ?: collectionId, "pending")
                            }
                            path == "/failure" || path?.contains("failure") == true -> {
                                val errorCode = uri.getQueryParameter("error_code")
                                val errorMessage = uri.getQueryParameter("error_message")
                                
                                onError("Pagamento rejeitado: $errorCode - $errorMessage")
                            }
                            else -> {
                                onError("Status de pagamento desconhecido: $path")
                            }
                        }
                    }
                    else -> {
                        val scheme = uri.scheme
                        val host = uri.host
                        val path = uri.path
                        onError("Deep Link inválido: $scheme://$host$path")
                    }
                }
            } else {
                onError("Nenhum dado de pagamento recebido")
            }
        } catch (e: Exception) {
            onError("Erro ao processar resultado: ${e.message}")
        }
    }
    
    /**
     * Valida assinatura consultando Mercado Pago REAL
     */
    fun validateSubscription(
        paymentId: String,
        onResult: (isValid: Boolean, expiryDate: Long?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val paymentData = getPaymentFromMercadoPago(paymentId)
                val isValid = paymentData?.get("status") == "approved"
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
    
    /**
     * Consulta pagamento DIRETO no Mercado Pago - SEM BACKEND!
     */
    private suspend fun getPaymentFromMercadoPago(paymentId: String): Map<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$MP_PAYMENTS_URL/$paymentId")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $ACCESS_TOKEN")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val responseJson = JSONObject(response)
                    
                    val paymentData: Map<String, String> = mapOf(
                        "id" to responseJson.getString("id"),
                        "status" to responseJson.getString("status"),
                        "status_detail" to responseJson.optString("status_detail", ""),
                        "payment_method_id" to (responseJson.optJSONObject("payment_method")?.optString("id") ?: ""),
                        "transaction_amount" to responseJson.optDouble("transaction_amount", 0.0).toString()
                    )
                    
                    paymentData
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Testa as credenciais do Mercado Pago fazendo uma requisição simples
     */
    fun testMercadoPagoCredentials(onResult: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$MP_API_BASE/v1/payment_methods")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $ACCESS_TOKEN")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                
                withContext(Dispatchers.Main) {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val responseArray = org.json.JSONArray(response)
                        val methodsCount = responseArray.length()
                        
                        onResult(true, "Credenciais válidas - $methodsCount métodos de pagamento disponíveis")
                    } else {
                        val errorResponse = try {
                            connection.errorStream?.bufferedReader()?.readText() ?: "Sem resposta de erro"
                        } catch (e: Exception) {
                            "Erro ao ler resposta: ${e.message}"
                        }
                        
                        onResult(false, "HTTP $responseCode: $errorResponse")
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Erro de conexão: ${e.javaClass.simpleName} - ${e.message}")
                }
            }
        }
    }

}
