package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var emptyBookingsMessage: TextView
    private lateinit var emptyBookingsImage: ImageView
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
        emptyBookingsMessage = view.findViewById(R.id.emptyBookingsMessage)
        emptyBookingsImage = view.findViewById(R.id.emptyBookingsImage)
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
            bookings = listOf(),
            onConfirmClick = { booking -> confirmBooking(booking) },
            onCancelClick = { booking -> cancelBooking(booking) },
            onEmptyList = { showEmptyBookingsMessage(true) } // Callback para lista vazia
        )

        oldBookingAdapter = ClientBookingAdapter(
            bookings = listOf(),
            onConfirmClick = { booking -> confirmBooking(booking) },
            onCancelClick = { booking -> cancelBooking(booking) },
            onEmptyList = { showEmptyBookingsMessage(true) } // Callback para lista vazia
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
                    Log.e("BookingFragment", "Erro ao buscar agendamentos: ${error.message}")
                    Toast.makeText(
                        requireContext(),
                        "Erro ao buscar agendamentos.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

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
        if (newBookings.isEmpty() && oldBookings.isEmpty()) {
            showEmptyBookingsMessage(true)
        } else {
            showEmptyBookingsMessage(false)

            newBookingsTitle.visibility = if (newBookings.isNotEmpty()) View.VISIBLE else View.GONE
            oldBookingsTitle.visibility = if (oldBookings.isNotEmpty()) View.VISIBLE else View.GONE

            newBookingsRecyclerView.visibility =
                if (newBookings.isNotEmpty()) View.VISIBLE else View.GONE
            oldBookingsRecyclerView.visibility =
                if (oldBookings.isNotEmpty()) View.VISIBLE else View.GONE

            newBookingAdapter.updateData(newBookings)
            oldBookingAdapter.updateData(oldBookings)
        }
    }

    private fun showEmptyBookingsMessage(show: Boolean) {
        emptyBookingsMessage.visibility = if (show) View.VISIBLE else View.GONE
        emptyBookingsImage.visibility = if (show) View.VISIBLE else View.GONE
        newBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        oldBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
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
                Log.d("FCM", "Token FCM recebido: $fcmToken")  // Log para verificar o token

                if (!fcmToken.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        sendFCMNotification(fcmToken, title, message)
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
            TokenUtils.getAccessTokenFromServiceAccount(requireContext())
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
            Volley.newRequestQueue(requireContext()).add(request)
        }
    }
}
