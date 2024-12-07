package com.conect.aplicativoconect.view.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.conect.aplicativoconect.view.TokenUtils
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ClientViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?> get() = _userName

    private val _userImage = MutableLiveData<String?>()
    val userImage: LiveData<String?> get() = _userImage

    private val _todayBookings = MutableLiveData<List<Booking>>()
    val todayBookings: LiveData<List<Booking>> get() = _todayBookings

    private val _userEmail = MutableLiveData<String?>()
    val userEmail: LiveData<String?> get() = _userEmail

    // Carrega dados do usuário
    fun loadUserData(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                _userName.value = document.getString("name")
                _userImage.value = document.getString("imageUrl")
                _userEmail.value = document.getString("email")
            }
            .addOnFailureListener { e ->
                Log.e("ClientViewModel", "Erro ao carregar dados do usuário: ${e.message}")
            }
    }

    // Carrega agendamentos futuros do usuário
    fun fetchUserBookings(userId: String) {
        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .orderBy("date")
            .orderBy("hour")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("ClientViewModel", "Erro ao buscar agendamentos: ${error.message}")
                    return@addSnapshotListener
                }

                val bookings = querySnapshot?.documents?.mapNotNull {
                    it.toObject(Booking::class.java)?.apply { id = it.id }
                }.orEmpty()

                // Filtra os agendamentos futuros
                val futureBookings = bookings.filter { isFutureBooking(it) }

                // Limita a 2 próximos agendamentos
                val nextTwoBookings = futureBookings.take(2)

                // Atualiza a LiveData com os 2 próximos agendamentos futuros
                _todayBookings.value = nextTwoBookings
                Log.d("ClientViewModel", "Agendamentos futuros: ${nextTwoBookings.size}")
            }
    }

    // Confirmação de agendamento
    fun confirmBooking(booking: Booking, context: Context) {
        firestore.collection("bookings").document(booking.id!!)
            .update("status_cliente", "confirmed")
            .addOnSuccessListener {
                sendBookingNotification(
                    context,
                    booking.id!!,
                    "Agendamento Confirmado",
                    "Olá, o agendamento de ${booking.name} foi confirmado pelo cliente!"
                )
            }
            .addOnFailureListener { e ->
                Log.e("ClientViewModel", "Erro ao confirmar agendamento: ${e.message}")
            }
    }

    // Função para cancelar o agendamento
    fun cancelBooking(booking: Booking, context: Context) {
        firestore.collection("bookings").document(booking.id!!)
            .delete()
            .addOnSuccessListener {
                sendBookingNotification(
                    context,
                    booking.id!!,
                    "Agendamento Cancelado",
                    "Olá, o agendamento de ${booking.name} foi cancelado!"
                )
            }
            .addOnFailureListener { e ->
                Log.e("ClientViewModel", "Erro ao cancelar agendamento: ${e.message}")
            }
    }

    // Função para enviar notificações
    private fun sendBookingNotification(context: Context, bookingId: String, title: String, message: String) {
        firestore.collection("bookings").document(bookingId)
            .get()
            .addOnSuccessListener { bookingDocument ->
                val companyId = bookingDocument.getString("companyId") ?: return@addOnSuccessListener
                fetchBusinessToken(companyId) { adminToken ->
                    if (adminToken != null) {
                        sendFCMNotification(context, adminToken, title, message)
                    } else {
                        Log.e("FCM", "Token do administrador não encontrado para a empresa $companyId")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Erro ao buscar agendamento: ${e.message}")
            }
    }

    // Função para buscar o token do administrador
    private fun fetchBusinessToken(companyId: String, callback: (String?) -> Unit) {
        firestore.collection("business").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcmToken")
                Log.d("FCM", "Token do administrador para empresa $companyId: $token")
                callback(token)
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Erro ao buscar token do administrador para empresa $companyId: ${e.message}")
                callback(null)
            }
    }

    // Função para enviar a notificação FCM
    private fun sendFCMNotification(context: Context, token: String, title: String, message: String) {
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
                TokenUtils.getAccessTokenFromServiceAccount(context)
            }

            if (accessToken == null) {
                Log.e("FCM", "Falha ao obter o token de acesso.")
                return@launch
            }

            val request = object : StringRequest(
                Method.POST, url,
                Response.Listener { response -> Log.d("FCM", "Notificação enviada com sucesso: $response") },
                Response.ErrorListener { error -> Log.e("FCM", "Erro ao enviar notificação: ${error.message}") }
            ) {
                override fun getHeaders(): Map<String, String> {
                    return mapOf(
                        "Authorization" to "Bearer $accessToken",
                        "Content-Type" to "application/json"
                    )
                }

                override fun getBody(): ByteArray = payload.toByteArray(Charsets.UTF_8)
            }

            withContext(Dispatchers.Main) {
                Volley.newRequestQueue(context).add(request)
            }
        }
    }

    // Verifica se o agendamento é futuro
    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDateTime = Calendar.getInstance().time
        val bookingDateTime = parseDateTime(booking.date ?: "", booking.hour)
        return bookingDateTime?.after(currentDateTime) ?: false
    }

    // Parse da data e hora do agendamento
    private fun parseDateTime(date: String, hour: String): Date? {
        return try {
            val dateParts = date.split("/").map { it.toInt() }
            val timeParts = hour.split(":").map { it.toIntOrNull() ?: 0 }

            Calendar.getInstance().apply {
                set(Calendar.YEAR, dateParts[2])
                set(Calendar.MONTH, dateParts[1] - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[0])
                set(Calendar.HOUR_OF_DAY, timeParts[0])
                set(Calendar.MINUTE, timeParts.getOrElse(1) { 0 })
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        } catch (e: Exception) {
            null
        }
    }

    // Salvamento das avaliações
    fun saveRatings(
        context: Context,
        bookingId: String?,
        quality: Int,
        punctuality: Int,
        service: Int
    ) {
        if (bookingId == null) return
        val ratingData = hashMapOf(
            "quality" to quality,
            "punctuality" to punctuality,
            "service" to service,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("bookings").document(bookingId)
            .update("rating", ratingData)
            .addOnSuccessListener {
                firestore.collection("bookings").document(bookingId).get()
                    .addOnSuccessListener { bookingSnapshot ->
                        val companyId = bookingSnapshot.getString("companyId")
                        if (!companyId.isNullOrEmpty()) {
                            updateBusinessRating(companyId, (quality + punctuality + service) / 3.0)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao salvar avaliação", Toast.LENGTH_SHORT).show()
                Log.e("ClientViewModel", "Erro ao salvar avaliação: ${e.message}")
            }
    }

    // Atualização da média de avaliação da empresa
    private fun updateBusinessRating(companyId: String, newRating: Double) {
        val businessRef = firestore.collection("business").document(companyId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(businessRef)
            val currentRating = snapshot.getDouble("averageRating") ?: 0.0
            val ratingCount = snapshot.getLong("ratingCount")?.toInt() ?: 0

            val updatedRatingCount = ratingCount + 1
            val updatedAverageRating =
                (currentRating * ratingCount + newRating) / updatedRatingCount

            transaction.update(businessRef, "averageRating", updatedAverageRating)
            transaction.update(businessRef, "ratingCount", updatedRatingCount)
        }.addOnSuccessListener {
            Log.d("ClientViewModel", "Média de avaliação da empresa atualizada com sucesso.")
        }.addOnFailureListener { e ->
            Log.e("ClientViewModel", "Erro ao atualizar média de avaliação: ${e.message}")
        }
    }
}
