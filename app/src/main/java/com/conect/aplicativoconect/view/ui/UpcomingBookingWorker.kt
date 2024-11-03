package com.conect.aplicativoconect.view.ui

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.conect.aplicativoconect.view.TokenUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class UpcomingBookingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        Log.d("UpcomingBookingWorker", "Worker iniciou para verificar agendamentos.")
        checkUpcomingBookings()
        return Result.success()
    }

    private fun checkUpcomingBookings() {
        val currentTimeMillis = Calendar.getInstance().timeInMillis
        val timeWindow = currentTimeMillis + TimeUnit.HOURS.toMillis(10)

        firestore.collection("bookings")
            .whereEqualTo("status_adm", "confirmed")
            .whereEqualTo("notified", false) // Apenas os não notificados
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("UpcomingBookingWorker", "Agendamentos obtidos com sucesso.")
                querySnapshot.documents.forEach { document ->
                    val bookingDateStr = document.getString("date") ?: return@forEach
                    val bookingHourStr = document.getString("hour") ?: return@forEach
                    val bookingId = document.id
                    val userId = document.getString("userId") ?: return@forEach
                    val serviceName = document.getString("serviceName") ?: "Serviço"

                    val bookingDateMillis = parseDateTimeToMillis(bookingDateStr, bookingHourStr)

                    if (bookingDateMillis != null && bookingDateMillis in currentTimeMillis until timeWindow) {
                        Log.d("UpcomingBookingWorker", "Preparando para enviar notificação para o agendamento $bookingId.")
                        val notificationMessage = "Seu agendamento para $serviceName é em breve, às ${bookingHourStr}h!"
                        sendNotificationToUser(
                            userId, "Lembrete de Agendamento", notificationMessage
                        )
                        markBookingAsNotified(bookingId) // Marca o agendamento como notificado
                    } else {
                        Log.d("UpcomingBookingWorker", "Agendamento $bookingId fora do intervalo para notificação.")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("UpcomingBookingWorker", "Erro ao buscar agendamentos: ${e.message}")
            }
    }

    private fun parseDateTimeToMillis(dateStr: String, hourStr: String): Long? {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateTimeStr = "$dateStr $hourStr"
            val date = dateFormat.parse(dateTimeStr)
            Log.d("UpcomingBookingWorker", "Data e hora convertida: $date para agendamento.")
            date?.time
        } catch (e: Exception) {
            Log.e("UpcomingBookingWorker", "Erro ao converter data/hora: ${e.message}")
            null
        }
    }

    private fun sendNotificationToUser(userId: String, title: String, message: String) {
        fetchUserToken(userId) { token ->
            if (token != null) {
                Log.d("UpcomingBookingWorker", "Enviando notificação para o usuário com token: $token")
                sendFCMNotification(token, title, message)
            } else {
                Log.e("UpcomingBookingWorker", "Token de usuário para $userId não encontrado.")
            }
        }
    }

    private fun sendNotificationToAdmin(bookingId: String, title: String, message: String) {
        firestore.collection("business").document(bookingId)
            .get()
            .addOnSuccessListener { document ->
                val ownerId = document.getString("ownerId") ?: return@addOnSuccessListener
                fetchUserToken(ownerId) { token ->
                    if (token != null) {
                        Log.d("UpcomingBookingWorker", "Enviando notificação para o administrador com token: $token")
                        sendFCMNotification(token, title, message)
                    } else {
                        Log.e("UpcomingBookingWorker", "Token do administrador para $ownerId não encontrado.")
                    }
                }
            }
    }

    private fun fetchUserToken(userId: String, callback: (String?) -> Unit) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcmToken")
                Log.d("UpcomingBookingWorker", "Token obtido para o usuário $userId: $token")
                callback(token)
            }
            .addOnFailureListener { e ->
                Log.e("UpcomingBookingWorker", "Erro ao buscar token do usuário: ${e.message}")
                callback(null)
            }
    }

    private fun sendFCMNotification(token: String, title: String, message: String) {
        val url = "https://fcm.googleapis.com/v1/projects/aplicativo-conect-f253d/messages:send"
        val payload = """
            {
              "message": {
                "token": "$token",
                "notification": {
                  "title": "$title",
                  "body": "$message"
                },
                "android": {
                  "priority": "high"
                }
              }
            }
        """.trimIndent()

        CoroutineScope(Dispatchers.IO).launch {
            val accessToken = withContext(Dispatchers.IO) {
                TokenUtils.getAccessTokenFromServiceAccount(applicationContext)
            }

            if (accessToken == null) {
                Log.e("FCM", "Falha ao obter o token de acesso.")
                return@launch
            }

            val request = object : StringRequest(
                Method.POST, url,
                Response.Listener { response ->
                    Log.d("FCM", "Notificação enviada com sucesso: $response")
                },
                Response.ErrorListener { error ->
                    Log.e("FCM", "Erro ao enviar notificação: ${error.message}")
                }
            ) {
                override fun getHeaders(): Map<String, String> {
                    return mapOf(
                        "Authorization" to "Bearer $accessToken",
                        "Content-Type" to "application/json"
                    )
                }

                override fun getBody(): ByteArray = payload.toByteArray(Charsets.UTF_8)
            }

            Volley.newRequestQueue(applicationContext).add(request)
        }
    }

    // Marcar o booking como notificado após o envio
    private fun markBookingAsNotified(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .update("notified", true)
            .addOnSuccessListener {
                Log.d("UpcomingBookingWorker", "Agendamento $bookingId marcado como notificado.")
            }
            .addOnFailureListener { e ->
                Log.e("UpcomingBookingWorker", "Erro ao marcar o agendamento como notificado: ${e.message}")
            }
    }
}
