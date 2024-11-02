package com.conect.aplicativoconect.view.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private val client = OkHttpClient.Builder().apply {
        addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer TEST-4420457494967421-103123-921492acbeaeb11cc125a32eec719ef7-474980834") // Token de acesso do Mercado Pago
                .build()
            chain.proceed(request)
        }
    }.build()

    val api: MercadoPagoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.mercadopago.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MercadoPagoApi::class.java)
    }


}