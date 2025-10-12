package com.conect.aplicativoconect.core.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.conect.aplicativoconect.utils.TokenManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.conect.aplicativoconect.utils.Validator
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

        // ✅ OTIMIZAÇÃO: Adicionar limite para evitar sobrecarga
        firestore.collection("bookings")
            .whereEqualTo("status_adm", "confirmed")
            .whereEqualTo("notified", false) 
            .limit(50) // Processa no máximo 50 agendamentos por execução
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("UpcomingBookingWorker", "Agendamentos obtidos com sucesso.")
                querySnapshot.documents.forEach { document ->
                    val bookingDateStr = document.getString("date") ?: return@forEach
                    val bookingHourStr = document.getString("hour") ?: return@forEach
                    val bookingId = document.id
                    val userId = document.getString("userId") ?: return@forEach
                    val companyId = document.getString("companyId") ?: return@forEach
                    val serviceName = document.getString("serviceName") ?: "Serviço"

                    val bookingDateMillis = parseDateTimeToMillis(bookingDateStr, bookingHourStr)

                    if (bookingDateMillis != null && bookingDateMillis in currentTimeMillis until timeWindow) {
                        Log.d(
                            "UpcomingBookingWorker",
                            "Preparando para enviar notificação para o agendamento $bookingId."
                        )

                        val userNotificationMessage =
                            "Seu agendamento para $serviceName é em breve, às ${bookingHourStr}h!"

                        val adminNotificationMessage =
                            "Agendamento para $serviceName está próximo, às ${bookingHourStr}h!"

                        // Enviar notificação para o usuário
                        sendNotificationToUser(
                            userId,
                            "Lembrete de Agendamento",
                            userNotificationMessage,
                            bookingId // Pass the bookingId
                        )

                        // Enviar notificação para o administrador
                        sendNotificationToAdmin(
                            companyId,
                            "Lembrete de Agendamento",
                            adminNotificationMessage,
                            bookingId // Pass the bookingId
                        )

                        // Marca o agendamento como notificado
                        markBookingAsNotified(bookingId)
                    } else {
                        Log.d(
                            "UpcomingBookingWorker",
                            "Agendamento $bookingId fora do intervalo para notificação."
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("UpcomingBookingWorker", "Erro ao buscar agendamentos: ${e.message}")
            }
    }


    private fun parseDateTimeToMillis(dateStr: String, hourStr: String): Long? {
        return try {
            val dateTimeStr = "$dateStr $hourStr"
            val date = Validator.DATE_TIME_FORMAT.parse(dateTimeStr) // ✅ OTIMIZADO: Formatador central
            Log.d("UpcomingBookingWorker", "Data e hora convertida: $date para agendamento.")
            date?.time
        } catch (e: Exception) {
            Log.e("UpcomingBookingWorker", "Erro ao converter data/hora: ${e.message}")
            null
        }
    }

    private fun sendNotificationToUser(
        userId: String,
        title: String,
        message: String,
        bookingId: String
    ) {
        fetchUserToken(userId) { token ->
            if (token != null) {
                Log.d(
                    "UpcomingBookingWorker",
                    "Enviando notificação para o usuário com token: $token"
                )
                sendFCMNotification(token, title, message) { success ->
                    if (success) {
                        // Atualizar o campo notified para true apenas se a notificação for enviada com sucesso
                        markBookingAsNotified(bookingId)
                    }
                }
            } else {
                Log.e("UpcomingBookingWorker", "Token de usuário para $userId não encontrado.")
            }
        }
    }

    private fun sendNotificationToAdmin(
        companyId: String,
        title: String,
        message: String,
        bookingId: String
    ) {
        firestore.collection("business").document(companyId)
            .get()
            .addOnSuccessListener { businessDocument ->
                val adminToken = businessDocument.getString("fcmToken")
                if (adminToken != null) {
                    Log.d(
                        "UpcomingBookingWorker",
                        "Enviando notificação para o administrador com token: $adminToken"
                    )
                    sendFCMNotification(adminToken, title, message) { success ->
                        if (success) {
                            // Atualizar o campo notified para true apenas se a notificação for enviada com sucesso
                            markBookingAsNotified(bookingId)
                        }
                    }
                } else {
                    Log.e(
                        "UpcomingBookingWorker",
                        "Token do administrador para $companyId não encontrado."
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("UpcomingBookingWorker", "Erro ao buscar dados do negócio: ${e.message}")
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

    private fun sendFCMNotification(
        token: String,
        title: String,
        message: String,
        callback: (Boolean) -> Unit
    ) {
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
            // Para notificações FCM, usamos o server key diretamente
            // O access token do service account seria usado para APIs do Google Cloud
            // ✅ Configurar server key FCM real no BuildConfig ou arquivo de configuração
            val serverKey = "YOUR_FCM_SERVER_KEY" // Substitua pela sua chave do servidor FCM

            val request = object : StringRequest(
                Method.POST, url,
                Response.Listener { response ->
                    Log.d("FCM", "Notificação enviada com sucesso: $response")
                    callback(true) // Notify success
                },
                Response.ErrorListener { error ->
                    Log.e("FCM", "Erro ao enviar notificação: ${error.message}")
                    callback(false) // Notify failure
                }
            ) {
                override fun getHeaders(): Map<String, String> {
                    return mapOf(
                        "Authorization" to "key=$serverKey",
                        "Content-Type" to "application/json"
                    )
                }

                override fun getBody(): ByteArray = payload.toByteArray(Charsets.UTF_8)
            }

            Volley.newRequestQueue(applicationContext).add(request)
        }
    }

    private fun markBookingAsNotified(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .update("notified", true)
            .addOnSuccessListener {
                Log.d("UpcomingBookingWorker", "Agendamento $bookingId marcado como notificado.")
            }
            .addOnFailureListener { e ->
                Log.e(
                    "UpcomingBookingWorker",
                    "Erro ao marcar o agendamento como notificado: ${e.message}"
                )
            }
    }
}