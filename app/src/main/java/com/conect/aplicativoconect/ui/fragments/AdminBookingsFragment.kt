package com.conect.aplicativoconect.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Booking
import com.conect.aplicativoconect.ui.adapters.BookingAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class AdminBookingsFragment : Fragment() {

    private lateinit var bookingsRecyclerView: RecyclerView
    private lateinit var bookingsAdapter: BookingAdapter
    private lateinit var emptyBookingsMessage: TextView
    private lateinit var emptyBookingsImage: ImageView
    private lateinit var bookingsTitle: TextView
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_bookings, container, false)

        // Inicializar as views
        bookingsRecyclerView = view.findViewById(R.id.recyclerViewBookings)
        emptyBookingsMessage = view.findViewById(R.id.emptyBookingsMessage)
        emptyBookingsImage = view.findViewById(R.id.emptyBookingsImage)
        bookingsTitle = view.findViewById(R.id.bookingsTitle)

        setupRecyclerView()
        loadBookings()

        return view
    }

    private fun setupRecyclerView() {
        bookingsRecyclerView.layoutManager = LinearLayoutManager(context)

        bookingsAdapter = BookingAdapter(
            bookings = listOf(),
            context = requireContext(),
            onConfirmBooking = { bookingId -> confirmBooking(bookingId) },
            onCancelBooking = { bookingId -> cancelBooking(bookingId) }
        )

        bookingsRecyclerView.adapter = bookingsAdapter
    }

    private fun loadBookings() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.e("AdminBookingsFragment", "Usuário não autenticado.")
            return
        }

        Log.d("AdminBookingsFragment", "Buscando agendamentos para o UID: $currentUserUid")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Consulta diretamente na coleção 'bookings' usando o currentUserUid como companyId
                val bookingsSnapshot = firestore.collection("bookings")
                    .whereEqualTo("companyId", currentUserUid)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val bookings = bookingsSnapshot.documents.mapNotNull { document ->
                    val booking = document.toObject(Booking::class.java)
                    booking?.id = document.id
                    booking
                }

                Log.d("AdminBookingsFragment", "Agendamentos encontrados: ${bookings.size}")

                withContext(Dispatchers.Main) {
                    if (bookings.isEmpty()) {
                        Log.d("AdminBookingsFragment", "Nenhum agendamento encontrado.")
                        showEmptyMessage(true)
                    } else {
                        Log.d("AdminBookingsFragment", "Exibindo ${bookings.size} agendamentos.")
                        bookingsAdapter.updateData(bookings)
                        showEmptyMessage(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminBookingsFragment", "Erro ao carregar agendamentos: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showEmptyMessage(true)
                }
            }
        }
    }

    private fun showEmptyMessage(show: Boolean) {
        if (show) {
            Log.d("AdminBookingsFragment", "Exibindo mensagem de vazio.")
            bookingsRecyclerView.visibility = View.GONE
            bookingsTitle.visibility = View.GONE
            view?.findViewById<View>(R.id.emptyBookingsLayout)?.visibility = View.VISIBLE
        } else {
            Log.d("AdminBookingsFragment", "Ocultando mensagem de vazio.")
            bookingsRecyclerView.visibility = View.VISIBLE
            bookingsTitle.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.emptyBookingsLayout)?.visibility = View.GONE
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

    private fun confirmBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .update("status_adm", "confirmed")
            .addOnSuccessListener {
                loadBookings()
            }
            .addOnFailureListener { e ->
                Log.w("AdminBookingsFragment", "Erro ao confirmar agendamento", e)
            }
    }

    private fun cancelBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .update("status_adm", "cancelled")
            .addOnSuccessListener {
                loadBookings()
            }
            .addOnFailureListener { e ->
                Log.w("AdminBookingsFragment", "Erro ao cancelar agendamento", e)
            }
    }
}
