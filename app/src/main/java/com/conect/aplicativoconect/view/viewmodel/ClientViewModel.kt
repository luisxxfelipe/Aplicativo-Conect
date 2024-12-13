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
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val _refreshBookings = MutableLiveData<Boolean>()
    val refreshBookings: LiveData<Boolean> get() = _refreshBookings


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
            }
    }

    // Carrega agendamentos futuros do usuário
    fun fetchUserBookings(userId: String) {
        val currentTimestamp = System.currentTimeMillis()
        Log.d(
            "ClientViewModel",
            "Fetching bookings for userId: $userId with timestamp >= $currentTimestamp"
        )

        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", currentTimestamp)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val bookings = querySnapshot.documents.mapNotNull {
                    it.toObject(Booking::class.java)?.apply { id = it.id }
                }
                Log.d("ClientViewModel", "Fetched ${bookings.size} bookings")
                _todayBookings.value = bookings // Atualiza LiveData mesmo que vazio
            }
            .addOnFailureListener { e ->
                Log.e("ClientViewModel", "Error fetching bookings: ${e.message}")
                _todayBookings.value = emptyList() // Define lista vazia em caso de erro
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
                    "Olá, o agendamento de ${booking.name} foi confirmado!"
                )
                _refreshBookings.value = true
            }
            .addOnFailureListener { e ->
            }
    }

    // Função para cancelar o agendamento
    fun cancelBooking(booking: Booking, context: Context) {
        // Envia a notificação antes de deletar
        sendBookingNotification(
            context,
            booking.id!!,
            "Agendamento Cancelado",
            "Olá, o agendamento de ${booking.name} foi cancelado!"
        )

        // Aguarda 3 segundos antes de deletar o agendamento
        CoroutineScope(Dispatchers.IO).launch {
            delay(3000) // Aguarda 3 segundos
            withContext(Dispatchers.Main) {
                firestore.collection("bookings").document(booking.id!!)
                    .delete()
                    .addOnSuccessListener {
                        _refreshBookings.value = true
                    }
                    .addOnFailureListener { e ->
                    }
            }
        }
    }


    // Função para enviar notificações
    private fun sendBookingNotification(
        context: Context,
        bookingId: String,
        title: String,
        message: String
    ) {
        firestore.collection("bookings").document(bookingId)
            .get()
            .addOnSuccessListener { bookingDocument ->
                val companyId =
                    bookingDocument.getString("companyId") ?: return@addOnSuccessListener
                fetchBusinessToken(companyId) { adminToken ->
                    if (adminToken != null) {
                        sendFCMNotification(context, adminToken, title, message)
                    }
                }
            }
            .addOnFailureListener { e ->
            }
    }

    // Função para buscar o token do administrador
    private fun fetchBusinessToken(companyId: String, callback: (String?) -> Unit) {
        firestore.collection("business").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcmToken")
                callback(token)
            }
            .addOnFailureListener { e ->
                callback(null)
            }
    }

    // Função para enviar a notificação FCM
    private fun sendFCMNotification(
        context: Context,
        token: String,
        title: String,
        message: String
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
            val accessToken = withContext(Dispatchers.IO) {
                TokenUtils.getAccessTokenFromServiceAccount(context)
            }

            if (accessToken == null) {
                return@launch
            }

            val request = object : StringRequest(
                Method.POST, url,
                Response.Listener { response ->
                },
                Response.ErrorListener { error ->
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

            withContext(Dispatchers.Main) {
                Volley.newRequestQueue(context).add(request)
            }
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
        }.addOnFailureListener { e ->
        }
    }
}
