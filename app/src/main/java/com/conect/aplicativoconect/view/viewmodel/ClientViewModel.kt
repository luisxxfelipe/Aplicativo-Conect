package com.conect.aplicativoconect.view.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.conect.aplicativoconect.view.TokenUtils
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

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
                _userEmail.value = document.getString("email")  // Adicione esta linha
            }
            .addOnFailureListener { e ->
                Log.e("ClientViewModel", "Erro ao carregar dados do usuário: ${e.message}")
            }
    }

    // Busca agendamentos futuros do usuário e limita a 2 próximos agendamentos
    fun fetchUserBookings(userId: String) {
        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("ClientViewModel", "Erro ao buscar agendamentos: ${error.message}")
                    return@addSnapshotListener
                }

                val bookings = querySnapshot?.documents?.mapNotNull {
                    it.toObject(Booking::class.java)?.apply { id = it.id }
                }.orEmpty()

                // Filtra e ordena para obter os 2 próximos agendamentos futuros
                val futureBookings = bookings.filter { isFutureBooking(it) }
                    .sortedBy { parseDateTime(it.date ?: "", it.hour) }
                    .take(2)

                _todayBookings.value = futureBookings
            }
    }

    // Confirmação de agendamento
    fun confirmBooking(context: Context, booking: Booking) {
        firestore.collection("bookings").document(booking.id!!)
            .update("status_cliente", "confirmed")
            .addOnSuccessListener {
                sendNotificationToBusiness(
                    context,
                    booking,
                    "Agendamento Confirmado",
                    "O agendamento de ${booking.name} foi confirmado!"
                )
                Toast.makeText(context, "Agendamento confirmado.", Toast.LENGTH_SHORT).show()
                booking.userId?.let { it1 -> fetchUserBookings(it1) }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao confirmar agendamento.", Toast.LENGTH_SHORT).show()
                Log.e("ClientViewModel", "Erro ao confirmar agendamento: ${e.message}")
            }
    }

    // Cancelamento de agendamento
    fun cancelBooking(context: Context, booking: Booking) {
        firestore.collection("bookings").document(booking.id!!)
            .delete()
            .addOnSuccessListener {
                sendNotificationToBusiness(
                    context,
                    booking,
                    "Agendamento Cancelado",
                    "O agendamento de ${booking.name} foi cancelado."
                )
                Toast.makeText(context, "Agendamento cancelado e excluído.", Toast.LENGTH_SHORT)
                    .show()
                booking.userId?.let { it1 -> fetchUserBookings(it1) }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao cancelar agendamento.", Toast.LENGTH_SHORT).show()
                Log.e("ClientViewModel", "Erro ao cancelar agendamento: ${e.message}")
            }
    }

    // Envio de notificação para o estabelecimento
    private fun sendNotificationToBusiness(
        context: Context,
        booking: Booking,
        title: String,
        message: String
    ) {
        val companyId = booking.companyId ?: return

        firestore.collection("business").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")
                if (!fcmToken.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        sendFCMNotification(context, fcmToken, title, message)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClientViewModel", "Erro ao buscar token FCM: ${e.message}")
            }
    }

    private suspend fun sendFCMNotification(
        context: Context,
        token: String,
        title: String,
        message: String
    ) {
        val url = "https://fcm.googleapis.com/v1/projects/aplicativo-conect-f253d/messages:send"
        val iconName = "ic_notification_icon"
        val payload = """
        {
          "message": {
            "token": "$token",
            "notification": {
              "title": "$title",
              "body": "$message",
              "icon": "$iconName"
            },
            "android": {
              "priority": "high"
            }
          }
        }
        """.trimIndent()

        val accessToken = withContext(Dispatchers.IO) {
            TokenUtils.getAccessTokenFromServiceAccount(context)
        }

        if (accessToken == null) {
            Log.e("ClientViewModel", "Erro ao obter token de acesso.")
            return
        }

        val request = object : StringRequest(
            Method.POST, url,
            { response -> Log.d("FCM", "Notificação enviada: $response") },
            { error -> Log.e("FCM", "Erro ao enviar notificação: ${error.message}") }
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

    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDateTime = Calendar.getInstance().time
        val bookingDateTime = parseDateTime(booking.date ?: "", booking.hour)
        return bookingDateTime?.after(currentDateTime) ?: false
    }

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
