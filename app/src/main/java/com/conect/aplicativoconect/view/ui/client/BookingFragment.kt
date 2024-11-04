package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.TokenUtils
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class BookingFragment : Fragment() {

    private lateinit var newBookingsRecyclerView: RecyclerView
    private lateinit var oldBookingsRecyclerView: RecyclerView
    private lateinit var newBookingAdapter: ClientBookingAdapter
    private lateinit var oldBookingAdapter: ClientBookingAdapter
    private lateinit var emptyBookingsLayout: LinearLayout
    private lateinit var newBookingsTitle: TextView
    private lateinit var oldBookingsTitle: TextView
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cliente_booking, container, false)

        newBookingsRecyclerView = view.findViewById(R.id.recyclerViewNewBookings)
        oldBookingsRecyclerView = view.findViewById(R.id.recyclerViewOldBookings)
        emptyBookingsLayout = view.findViewById(R.id.emptyBookingsLayout)
        newBookingsTitle = view.findViewById(R.id.newBookingsTitle)
        oldBookingsTitle = view.findViewById(R.id.oldBookingsTitle)

        setupRecyclerViews()
        loadBookings()

        return view
    }

    private fun adjustNestedScrollViewHeight() {
        val itemCount = newBookingAdapter.itemCount

        val params = newBookingsRecyclerView.layoutParams
        params.height = if (itemCount > 3) {
            resources.getDimensionPixelSize(R.dimen.fixed_height_for_3_items) // Defina como 420dp
        } else {
            RecyclerView.LayoutParams.WRAP_CONTENT
        }
        newBookingsRecyclerView.layoutParams = params
    }


    private fun setupRecyclerViews() {
        newBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
        oldBookingsRecyclerView.layoutManager = LinearLayoutManager(context)

        newBookingAdapter = ClientBookingAdapter(
            context = requireContext(),
            bookings = listOf(),
            onConfirmClick = { booking -> confirmBooking(booking) },
            onCancelClick = { booking -> cancelBooking(booking) },
            onRateClick = { booking -> showRatingPopup(booking) },
            onEmptyList = { showEmptyBookingsMessage(true) }
        )

        oldBookingAdapter = ClientBookingAdapter(
            context = requireContext(),
            bookings = listOf(),
            onConfirmClick = { booking -> confirmBooking(booking) },
            onCancelClick = { booking -> cancelBooking(booking) },
            onRateClick = { booking -> showRatingPopup(booking) },
            onEmptyList = { showEmptyBookingsMessage(true) }
        )

        newBookingsRecyclerView.adapter = newBookingAdapter
        oldBookingsRecyclerView.adapter = oldBookingAdapter
    }

    private fun loadBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    if (isAdded) { // Verifique se o fragmento ainda está anexado antes de usar o contexto
                        Toast.makeText(requireContext(), "Erro ao buscar agendamentos.", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                // Verifique novamente se o fragmento está anexado ao contexto antes de continuar
                if (!isAdded) return@addSnapshotListener

                val (newBookings, oldBookings) = querySnapshot?.documents?.mapNotNull { document ->
                    val booking = document.toObject(Booking::class.java)
                    booking?.id = document.id
                    booking
                }?.partition { isFutureBooking(it) } ?: Pair(emptyList(), emptyList())

                updateUI(newBookings, oldBookings)
                adjustNestedScrollViewHeight()
            }
    }

    private fun updateUI(newBookings: List<Booking>, oldBookings: List<Booking>) {
        // Atualiza os adaptadores primeiro
        newBookingAdapter.updateData(newBookings)
        oldBookingAdapter.updateData(oldBookings)

        // Verifica se há novos ou antigos agendamentos
        val hasNewBookings = newBookings.isNotEmpty()
        val hasOldBookings = oldBookings.isNotEmpty()

        // Define visibilidade dos títulos e RecyclerViews
        newBookingsTitle.visibility = if (hasNewBookings) View.VISIBLE else View.GONE
        oldBookingsTitle.visibility = if (hasOldBookings) View.VISIBLE else View.GONE
        newBookingsRecyclerView.visibility = if (hasNewBookings) View.VISIBLE else View.GONE
        oldBookingsRecyclerView.visibility = if (hasOldBookings) View.VISIBLE else View.GONE

        // Exibe a imagem e mensagem de vazio somente se ambos os RecyclerViews estiverem vazios
        showEmptyBookingsMessage(!hasNewBookings && !hasOldBookings)
    }

    private fun showEmptyBookingsMessage(show: Boolean) {
        emptyBookingsLayout.visibility = if (show) View.VISIBLE else View.GONE
    }


    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDateTime = Calendar.getInstance()

        // Verificar se `date` e `hour` estão presentes e processá-los
        val bookingDateParts = booking.date?.split("/")?.map { it.toIntOrNull() }
        val bookingHourParts = booking.hour?.split(":")?.map { it.toIntOrNull() }

        // Verificar se todos os elementos de data foram extraídos corretamente e se `hour` é válido
        if (bookingDateParts != null && bookingDateParts.size == 3 && bookingHourParts != null && bookingHourParts.size >= 1) {
            // Criar o calendário do agendamento com data e hora
            val bookingCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, bookingDateParts[2]!!)
                set(Calendar.MONTH, bookingDateParts[1]!! - 1)
                set(Calendar.DAY_OF_MONTH, bookingDateParts[0]!!)
                set(Calendar.HOUR_OF_DAY, bookingHourParts[0]!!)
                bookingHourParts.getOrElse(1) { 0 }?.let {
                    set(
                        Calendar.MINUTE,
                        it
                    )
                } // Caso `minute` não seja especificado, assumimos 0 minutos
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Comparar o agendamento com a data e hora atuais
            return bookingCalendar.after(currentDateTime)
        }
        return false
    }


    private fun confirmBooking(booking: Booking) {
        firestore.collection("bookings").document(booking.id!!)
            .update("status_cliente", "confirmed")
            .addOnSuccessListener {
                sendNotificationToBusiness(
                    booking,
                    "Agendamento Confirmado",
                    "Olá ${booking.name}, seu agendamento foi confirmado!"
                )
                Toast.makeText(requireContext(), "Agendamento confirmado.", Toast.LENGTH_SHORT)
                    .show()
                loadBookings()
            }
            .addOnFailureListener { e ->
                Log.e("BookingFragment", "Erro ao confirmar agendamento: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Erro ao confirmar agendamento.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun cancelBooking(booking: Booking) {
        firestore.collection("bookings").document(booking.id!!)
            .delete()
            .addOnSuccessListener {
                sendNotificationToBusiness(
                    booking,
                    "Agendamento Cancelado",
                    "O agendamento de ${booking.name} foi cancelado."
                )
                Toast.makeText(
                    requireContext(),
                    "Agendamento cancelado e excluído.",
                    Toast.LENGTH_SHORT
                ).show()
                loadBookings()
            }
            .addOnFailureListener { e ->
                Log.e("BookingFragment", "Erro ao cancelar agendamento: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Erro ao cancelar agendamento.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun sendNotificationToBusiness(booking: Booking, title: String, message: String) {
        val companyId = booking.companyId ?: return

        firestore.collection("business").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")
                Log.d("FCM", "Token FCM recebido: $fcmToken")

                if (!fcmToken.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (isAdded) {
                            sendFCMNotification(fcmToken, title, message)
                        }
                    }
                } else {
                    Log.e("FCM", "Token FCM não encontrado para empresa: $companyId")
                }
            }
            .addOnFailureListener { e ->
                Log.e("BookingFragment", "Erro ao buscar token FCM: ${e.message}")
            }
    }

    private suspend fun sendFCMNotification(token: String, title: String, message: String) {
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

        val accessToken = withContext(Dispatchers.IO) {
            context?.let { TokenUtils.getAccessTokenFromServiceAccount(it) }
        }

        if (accessToken == null) {
            Log.e("FCM", "Erro ao obter token de acesso.")
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
            context?.let { Volley.newRequestQueue(it).add(request) }
        }
    }

    private fun showRatingPopup(booking: Booking) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_rating, null)
        val ratingQuality = dialogView.findViewById<RatingBar>(R.id.ratingQuality)
        val ratingPunctuality = dialogView.findViewById<RatingBar>(R.id.ratingPunctuality)
        val ratingService = dialogView.findViewById<RatingBar>(R.id.ratingService)
        val commentEditText = dialogView.findViewById<EditText>(R.id.commentEditText) // Campo de comentário
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        saveButton.setOnClickListener {
            val qualityRating = ratingQuality.rating.toInt()
            val punctualityRating = ratingPunctuality.rating.toInt()
            val serviceRating = ratingService.rating.toInt()
            val comment = commentEditText.text.toString().takeIf { it.isNotBlank() }

            saveRatings(booking.id, qualityRating, punctualityRating, serviceRating, comment)
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun saveRatings(bookingId: String?, quality: Int, punctuality: Int, service: Int, comment: String?) {
        if (bookingId == null) return

        // Inclui o comentário no mapa de dados, mesmo que esteja vazio
        val ratingData = mutableMapOf<String, Any>(
            "quality" to quality,
            "punctuality" to punctuality,
            "service" to service,
            "timestamp" to System.currentTimeMillis(),
            "comment" to (comment ?: "")  // Garante que "comment" seja uma string, mesmo que vazia
        )

        firestore.collection("bookings").document(bookingId)
            .update("rating", ratingData)
            .addOnSuccessListener {
                Log.d("saveRatings", "Avaliação e comentário salvos com sucesso para bookingId: $bookingId")

                firestore.collection("bookings").document(bookingId).get()
                    .addOnSuccessListener { bookingSnapshot ->
                        val companyId = bookingSnapshot.getString("companyId")
                        Log.d("saveRatings", "companyId encontrado: $companyId")

                        if (!companyId.isNullOrEmpty()) {
                            val averageRating = (quality + punctuality + service) / 3.0
                            updateBusinessRating(companyId, averageRating)
                        } else {
                            Log.e("saveRatings", "companyId não encontrado para bookingId: $bookingId")
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("saveRatings", "Erro ao salvar avaliação: ${e.message}")
                Toast.makeText(requireContext(), "Erro ao salvar avaliação", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBusinessRating(companyId: String, newRating: Double) {
        val businessRef = firestore.collection("business").document(companyId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(businessRef)

            // Use as funções de conversão adequadas
            val currentRating = snapshot.getDouble("averageRating") ?: 0.0
            val ratingCount = (snapshot.getLong("ratingCount") ?: 0).toInt()

            // Calcule a nova média corretamente
            val updatedRatingCount = ratingCount + 1
            val updatedAverageRating = ((currentRating * ratingCount) + newRating) / updatedRatingCount

            transaction.update(businessRef, "averageRating", updatedAverageRating)
            transaction.update(businessRef, "ratingCount", updatedRatingCount)
        }.addOnSuccessListener {
            Log.d("updateBusinessRating", "Média de avaliação atualizada com sucesso para companyId: $companyId")
        }.addOnFailureListener { e ->
            Log.e("updateBusinessRating", "Erro ao atualizar média de avaliação: ${e.message}")
        }
    }

}
