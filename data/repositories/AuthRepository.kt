package com.conect.aplicativoconect.data.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.util.Log

class AuthRepository {
    private val db = FirebaseFirestore.getInstance()

    // ... existing code ...

    suspend fun checkSubscription(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("subscriptions").document(userId).get().await()
            if (!doc.exists()) return@withContext false

            val endDate = doc.getTimestamp("endDate")?.toDate() ?: return@withContext false
            val isActive = doc.getBoolean("isActive") ?: false
            val isTrial = doc.getBoolean("isTrialActive") ?: false

            val now = Date()
            val expiredTrial = isTrial && endDate.before(now)
            if (expiredTrial) {
                // Update para false se trial expirado
                db.collection("subscriptions").document(userId)
                    .update("isTrialActive", false, "isActive", false)
                    .await()
                return@withContext false
            }

            return@withContext isActive || (isTrial && endDate.after(now))
        } catch (e: Exception) {
            Log.e("AuthRepo", "Erro check subscription: ${e.message}")
            false
        }
    }

    suspend fun renewSubscription(userId: String, paymentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("subscriptions").document(userId).get().await()
            if (!doc.exists()) return@withContext false

            val currentEnd = doc.getTimestamp("endDate")?.toDate() ?: Date()
            val calendar = Calendar.getInstance(Locale.getDefault()).apply {
                time = currentEnd
                add(Calendar.MONTH, 1)  // +1 mês prático
            }
            val newEnd = Timestamp(calendar.time)

            db.collection("subscriptions").document(userId)
                .update(
                    "endDate", newEnd,
                    "isActive", true,
                    "isTrialActive", false,
                    "lastPaymentId", paymentId  // Log payment para audit
                )
                .await()

            Log.d("AuthRepo", "Subscription renovada para $userId com payment $paymentId")
            true
        } catch (e: Exception) {
            Log.e("AuthRepo", "Erro renew subscription: ${e.message}")
            false
        }
    }
}
