package com.conect.aplicativoconect.ui.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.conect.aplicativoconect.data.repositories.BookingRepository
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Booking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.conect.aplicativoconect.ui.activities.EmpresaDetalhesActivity
import com.conect.aplicativoconect.ui.adapters.ClientBookingAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.conect.aplicativoconect.utils.Validator
import com.conect.aplicativoconect.utils.AuthHelper
import java.util.Calendar
import java.util.Date

class BookingFragment : Fragment() {

    private lateinit var bookingsRecyclerView: RecyclerView
    private lateinit var bookingsAdapter: ClientBookingAdapter
    private lateinit var emptyBookingsLayout: LinearLayout
    private lateinit var bookingsTitle: TextView
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Adicionar no topo (assumindo BookingRepository injetado ou instance)
    private val bookingRepo = BookingRepository()  // Ou via ViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cliente_booking, container, false)

        bookingsRecyclerView = view.findViewById(R.id.recyclerViewBookings)
        emptyBookingsLayout = view.findViewById(R.id.emptyBookingsLayout)
        bookingsTitle = view.findViewById(R.id.bookingsTitle)

        setupRecyclerView()
        loadBookings()

        return view
    }

    private fun setupRecyclerView() {
        bookingsRecyclerView.layoutManager = LinearLayoutManager(context)

        bookingsAdapter = ClientBookingAdapter(
            context = requireContext(),
            bookings = listOf(),
            onConfirmClick = { booking -> confirmBooking(booking) },
            onCancelClick = { booking -> cancelBooking(booking) },
            onRateClick = { booking -> showRatingPopup(booking) },
            onEmptyList = { showEmptyBookingsMessage(true) },
            onCardClick = { companyId -> openCompanyDetails(companyId) }
        )

        bookingsRecyclerView.adapter = bookingsAdapter
    }

    private fun openCompanyDetails(companyId: String) {
        val intent = Intent(requireContext(), EmpresaDetalhesActivity::class.java).apply {
            putExtra("companyId", companyId)
        }
        startActivity(intent)
    }

    private fun loadBookings() {
        val userId = AuthHelper.getCurrentUserId() ?: return // ✅ OTIMIZADO: Helper centralizado
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bookings = bookingRepo.getUserBookings(userId)  // Query através do repository
                val sortedBookings = bookings.sortedWith(compareByDescending<Booking> {
                    parseDateTime(it.date ?: "", it.hour)
                })

                withContext(Dispatchers.Main) {
                    if (sortedBookings.isEmpty()) {
                        showEmptyBookingsMessage(true)
                    } else {
                        bookingsAdapter.updateData(sortedBookings)
                        bookingsRecyclerView.visibility = View.VISIBLE
                        bookingsTitle.visibility = View.VISIBLE
                        showEmptyBookingsMessage(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showEmptyBookingsMessage(true)
                }
            }
        }
    }


    private fun showEmptyBookingsMessage(show: Boolean) {
        emptyBookingsLayout.visibility = if (show) View.VISIBLE else View.GONE
        bookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        bookingsTitle.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDateTime = Calendar.getInstance().time
        val bookingDateTime = parseDateTime(booking.date ?: "", booking.hour)
        return bookingDateTime?.after(currentDateTime) ?: false
    }

    private fun parseDateTime(date: String, hour: String): Date? {
        return try {
            val dateTimeString = "$date $hour"
            Validator.DATE_TIME_FORMAT.parse(dateTimeString) // ✅ OTIMIZADO: Formatador central
        } catch (e: Exception) {
            null
        }
    }

    private fun confirmBooking(booking: Booking) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                bookingRepo.confirmBooking(booking.id!!)  // Via repo
                bookingRepo.sendNotification(
                    booking.id!!,
                    "Agendamento Confirmado",
                    "Mensagem"
                )  // Via repo
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Agendamento confirmado.", Toast.LENGTH_SHORT)
                        .show()
                    loadBookings()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao confirmar agendamento.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun cancelBooking(booking: Booking) {
        firestore.collection("bookings").document(booking.id!!)
            .delete()
            .addOnSuccessListener {
                sendNotification(
                    booking.id!!,
                    "Agendamento Cancelado",
                    "Olá, o agendamento de ${booking.name} foi cancelado!"
                )
                Toast.makeText(
                    requireContext(),
                    "Agendamento cancelado e excluído.",
                    Toast.LENGTH_SHORT
                ).show()
                loadBookings()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Erro ao cancelar agendamento.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    // Função para enviar notificações, com a opção de notificar o cliente ou o administrador
    private fun sendNotification(
        bookingId: String,
        title: String,
        message: String
    ) {
        // Buscar o token do administrador
        firestore.collection("bookings").document(bookingId)
            .get()
            .addOnSuccessListener { bookingDocument ->
                val companyId =
                    bookingDocument.getString("companyId") ?: return@addOnSuccessListener
                fetchBusinessToken(companyId) { adminToken ->
                    if (adminToken != null) {
                        sendFCMNotification(adminToken, title, message)
                    } else {
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

        // Aqui estamos lançando uma corrotina dentro de um escopo adequado
        CoroutineScope(Dispatchers.IO).launch {
            // Aqui dentro você pode usar withContext pois estamos dentro de uma corrotina
            val accessToken = withContext(Dispatchers.IO) {
                "temp_token"
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

    private fun showRatingPopup(booking: Booking) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_rating, null)
        val ratingQuality = dialogView.findViewById<RatingBar>(R.id.ratingQuality)
        val ratingPunctuality = dialogView.findViewById<RatingBar>(R.id.ratingPunctuality)
        val ratingService = dialogView.findViewById<RatingBar>(R.id.ratingService)
        val commentEditText =
            dialogView.findViewById<EditText>(R.id.commentEditText) // Campo de comentário
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Ajuste da cor do botão "Salvar"
        saveButton.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        ) // Definindo a cor da fonte como branca

        // Garantir que o fundo do botão seja roxo
        val buttonBackgroundColor = ContextCompat.getColor(requireContext(), R.color.roxo)
        saveButton.setBackgroundColor(buttonBackgroundColor) // Definindo o fundo do botão como roxo

        // Personalizar o fundo do diálogo e os textos
        dialog.setOnShowListener {
            // Personalizar o fundo do AlertDialog
            val background = dialog.window?.decorView
            background?.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            ) // Fundo branco

            // Ajuste da cor da fonte no EditText (comentário)
            commentEditText.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.cinza_escuro
                )
            ) // Cor cinza

            // Ajuste dos botões
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            ) // Cor branca para o texto do botão
            positiveButton?.setBackgroundColor(buttonBackgroundColor) // Cor de fundo do botão roxa

            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            ) // Cor branca para o texto do botão
            negativeButton?.setBackgroundColor(buttonBackgroundColor) // Cor de fundo do botão roxa
        }

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


    private fun saveRatings(
        bookingId: String?,
        quality: Int,
        punctuality: Int,
        service: Int,
        comment: String?
    ) {
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

                firestore.collection("bookings").document(bookingId).get()
                    .addOnSuccessListener { bookingSnapshot ->
                        val companyId = bookingSnapshot.getString("companyId")

                        if (!companyId.isNullOrEmpty()) {
                            val averageRating = (quality + punctuality + service) / 3.0
                            updateBusinessRating(companyId, averageRating)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao salvar avaliação", Toast.LENGTH_SHORT)
                    .show()
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
            val updatedAverageRating =
                ((currentRating * ratingCount) + newRating) / updatedRatingCount

            transaction.update(businessRef, "averageRating", updatedAverageRating)
            transaction.update(businessRef, "ratingCount", updatedRatingCount)
        }.addOnSuccessListener {

        }.addOnFailureListener { e ->
        }
    }

}
