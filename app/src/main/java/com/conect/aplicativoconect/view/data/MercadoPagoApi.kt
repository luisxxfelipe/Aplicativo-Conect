package com.conect.aplicativoconect.view.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MercadoPagoApi {
    @POST("checkout/preferences")
    suspend fun createPreference(@Body preference: PaymentPreference): Response<PreferenceResponse>
}
