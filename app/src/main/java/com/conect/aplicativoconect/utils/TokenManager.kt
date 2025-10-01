package com.conect.aplicativoconect.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TokenManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun saveTokenForUser(userId: String) = withContext(Dispatchers.IO) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            db.collection("users").document(userId)
                .update("fcmToken", token)
                .await()
            Log.d("TokenManager", "Token salvo para user $userId")
        } catch (e: Exception) {
            Log.e("TokenManager", "Erro ao salvar token para user: ${e.message}")
            // Fallback: set se não existir
            val token = FirebaseMessaging.getInstance().token.await()
            db.collection("users").document(userId)
                .set(hashMapOf("fcmToken" to token))
                .await()
        }
    }

    suspend fun saveTokenForBusiness(businessId: String) = withContext(Dispatchers.IO) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            db.collection("business").document(businessId)
                .update("fcmToken", token)
                .await()
            Log.d("TokenManager", "Token salvo para business $businessId")
        } catch (e: Exception) {
            Log.e("TokenManager", "Erro ao salvar token para business: ${e.message}")
        }
    }

    suspend fun saveToken(collection: String, id: String) {
        when (collection) {
            "users" -> saveTokenForUser(id)
            "business" -> saveTokenForBusiness(id)
        }
    }

    fun getCurrentToken() = FirebaseMessaging.getInstance().token.result
}
