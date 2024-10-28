package com.conect.aplicativoconect.view.ui

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationAPI {
    @POST("v1/projects/aplicativo-conect-f253d/messages:send")
    fun sendNotification(
        @Header("Authorization") authHeader: String,
        @Body notification: NotificationData
    ): Call<Void>
}