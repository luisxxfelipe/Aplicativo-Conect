package com.conect.aplicativoconect.view

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TokenUtils {
    suspend fun getAccessTokenFromServiceAccount(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream =
                    context.assets.open("aplicativo-conect-f253d-firebase-adminsdk-xcfgw-9dc183006f.json")
                val googleCredentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))  // Alteração aqui
                googleCredentials.refreshIfExpired()
                googleCredentials.accessToken.tokenValue
            } catch (e: Exception) {
                Log.e("FCM", "Erro ao carregar token: ${e.message}")
                null
            }
        }
    }
}