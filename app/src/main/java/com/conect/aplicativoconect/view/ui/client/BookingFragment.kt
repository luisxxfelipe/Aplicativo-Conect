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
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class BookingFragment : Fragment() {

    private lateinit var bookingRecyclerView: RecyclerView
    private lateinit var bookingAdapter: ClientBookingAdapter
    private lateinit var emptyBookingsMessage: TextView
    private lateinit var emptyBookingsImage: ImageView
    private var bookings: List<Booking> = listOf()
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cliente_booking, container, false)

        bookingRecyclerView = view.findViewById(R.id.recyclerViewClientBookings)
        emptyBookingsMessage = view.findViewById(R.id.emptyBookingsMessage)
        emptyBookingsImage = view.findViewById(R.id.emptyBookingsImage)

        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        loadBookings() // Carregar agendamentos do banco de dados

        return view
    }

    private fun setupRecyclerView() {
        bookingRecyclerView.layoutManager = LinearLayoutManager(context)
        bookingAdapter = ClientBookingAdapter(bookings) { booking ->
            // Implementar lógica para cancelar agendamento
            cancelBooking(booking)
        }
        bookingRecyclerView.adapter = bookingAdapter
    }

    private fun loadBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    bookings =
                        querySnapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
                    bookings =
                        bookings.filter { isFutureBooking(it) } // Filtrar agendamentos futuros

                    if (bookings.isEmpty()) {
                        emptyBookingsMessage.visibility = View.VISIBLE
                        emptyBookingsImage.visibility = View.VISIBLE
                        bookingRecyclerView.visibility = View.GONE
                    } else {
                        emptyBookingsMessage.visibility = View.GONE
                        emptyBookingsImage.visibility = View.GONE
                        bookingRecyclerView.visibility = View.VISIBLE
                        bookingAdapter.updateBookings(bookings)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BookingFragment", "Erro ao buscar agendamentos: ${e.message}")
                    Toast.makeText(requireContext(), "Erro ao buscar agendamentos.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val bookingDateParts = booking.date?.split("/")?.map { it.toInt() }
        if (bookingDateParts != null && bookingDateParts.size == 3) {
            val bookingCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, bookingDateParts[2]) // YYYY
                set(Calendar.MONTH, bookingDateParts[1] - 1) // MM (0-11)
                set(Calendar.DAY_OF_MONTH, bookingDateParts[0]) // DD
            }
            return bookingCalendar.after(currentDate)
        }
        return false
    }

    private fun cancelBooking(booking: Booking) {
        // Implementar lógica para cancelar agendamento
    }
}
