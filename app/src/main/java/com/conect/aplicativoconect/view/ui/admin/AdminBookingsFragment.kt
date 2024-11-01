package com.conect.aplicativoconect.view.ui.admin

import BookingAdapter
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
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.repository.BookingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class AdminBookingsFragment : Fragment() {

    private lateinit var newBookingsRecyclerView: RecyclerView
    private lateinit var oldBookingsRecyclerView: RecyclerView
    private lateinit var newBookingAdapter: BookingAdapter
    private lateinit var oldBookingAdapter: BookingAdapter
    private lateinit var emptyBookingsMessage: TextView
    private lateinit var emptyBookingsImage: ImageView
    private lateinit var newBookingsTitle: TextView
    private lateinit var oldBookingsTitle: TextView
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_bookings, container, false)

        // Inicializar as views
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

    private fun loadBookings() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obter o ID do negócio do admin
                val businessSnapshot = firestore.collection("business")
                    .whereEqualTo("ownerId", currentUserUid)
                    .get()
                    .await()

                val businessId = businessSnapshot.documents.firstOrNull()?.id

                if (businessId == null) {
                    withContext(Dispatchers.Main) { showErrorMessage("Empresa não encontrada.") }
                    return@launch
                }

                // Obter os agendamentos do negócio
                val bookingsSnapshot = firestore.collection("bookings")
                    .whereEqualTo("companyId", businessId)
                    .get()
                    .await()

                val (newBookings, oldBookings) = bookingsSnapshot.documents.mapNotNull { document ->
                    val booking = document.toObject(Booking::class.java)
                    booking?.id = document.id
                    booking
                }.partition { isFutureBooking(it) }

                withContext(Dispatchers.Main) {
                    updateUI(newBookings, oldBookings)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorMessage("Erro ao carregar agendamentos.")
                }
            }
        }
    }

    private fun adjustNestedScrollViewHeight() {
        val itemCount = newBookingAdapter.itemCount

        val params = newBookingsRecyclerView.layoutParams
        params.height = if (itemCount > 3) {
            resources.getDimensionPixelSize(R.dimen.fixed_height_for_3_items) // Defina 420dp no arquivo dimens.xml
        } else {
            RecyclerView.LayoutParams.WRAP_CONTENT
        }
        newBookingsRecyclerView.layoutParams = params
    }

    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDateTime = Calendar.getInstance()

        // Verifica se date e hour não são nulos
        val bookingDate = booking.date ?: return false
        val bookingHourStr = booking.hour ?: return false

        val bookingDateParts = bookingDate.split("/").mapNotNull { it.toIntOrNull() }
        val bookingTimeParts = bookingHourStr.split(":").mapNotNull { it.toIntOrNull() }

        // Certifique-se de que a data tem 3 partes e o horário tem 2 partes (hora e minuto)
        if (bookingDateParts.size == 3 && bookingTimeParts.size == 2) {
            val bookingCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, bookingDateParts[2])
                set(Calendar.MONTH, bookingDateParts[1] - 1)
                set(Calendar.DAY_OF_MONTH, bookingDateParts[0])
                set(Calendar.HOUR_OF_DAY, bookingTimeParts[0])  // Hora extraída de "hour"
                set(Calendar.MINUTE, bookingTimeParts[1])       // Minuto extraído de "hour"
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return bookingCalendar.after(currentDateTime)
        }
        return false
    }


    private fun updateUI(newBookings: List<Booking>, oldBookings: List<Booking>) {
        if (newBookings.isEmpty() && oldBookings.isEmpty()) {
            showEmptyMessage(true)
        } else {
            showEmptyMessage(false)

            newBookingsTitle.visibility = if (newBookings.isNotEmpty()) View.VISIBLE else View.GONE
            oldBookingsTitle.visibility = if (oldBookings.isNotEmpty()) View.VISIBLE else View.GONE

            newBookingsRecyclerView.visibility = if (newBookings.isNotEmpty()) View.VISIBLE else View.GONE
            oldBookingsRecyclerView.visibility = if (oldBookings.isNotEmpty()) View.VISIBLE else View.GONE

            newBookingAdapter.updateData(newBookings)
            oldBookingAdapter.updateData(oldBookings)

            adjustNestedScrollViewHeight()
        }
    }

    private fun showEmptyMessage(show: Boolean) {
        emptyBookingsMessage.visibility = if (show) View.VISIBLE else View.GONE
        emptyBookingsImage.visibility = if (show) View.VISIBLE else View.GONE
        newBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        oldBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showErrorMessage(message: String) {
        emptyBookingsMessage.text = message
        showEmptyMessage(true)
    }

    private fun setupRecyclerViews() {
        newBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
        oldBookingsRecyclerView.layoutManager = LinearLayoutManager(context)

        newBookingAdapter = BookingAdapter(
            bookings = listOf(),
            context = requireContext(),
            onConfirmBooking = { bookingId -> confirmBooking(bookingId) },
            onCancelBooking = { bookingId -> cancelBooking(bookingId) }
        )

        oldBookingAdapter = BookingAdapter(
            bookings = listOf(),
            context = requireContext(),
            onConfirmBooking = { bookingId -> confirmBooking(bookingId) },
            onCancelBooking = { bookingId -> cancelBooking(bookingId) }
        )

        newBookingsRecyclerView.adapter = newBookingAdapter
        oldBookingsRecyclerView.adapter = oldBookingAdapter
    }

    // Função para confirmar o agendamento
    private fun confirmBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .update("status_adm", "confirmed")
            .addOnSuccessListener {
                Log.d("AdminBookingsFragment", "Agendamento confirmado com sucesso!")
                loadBookings() // Recarrega os agendamentos
            }
            .addOnFailureListener { e ->
                Log.w("AdminBookingsFragment", "Erro ao confirmar agendamento", e)
            }
    }

    // Função para cancelar o agendamento
    private fun cancelBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .delete()
            .addOnSuccessListener {
                Log.d("AdminBookingsFragment", "Agendamento cancelado com sucesso!")
                loadBookings() // Recarrega os agendamentos após o cancelamento
            }
            .addOnFailureListener { e ->
                Log.w("AdminBookingsFragment", "Erro ao cancelar agendamento", e)
            }
    }
}
