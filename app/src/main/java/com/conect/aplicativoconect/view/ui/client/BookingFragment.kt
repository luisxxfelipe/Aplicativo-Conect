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

        // Passando o contexto para o adapter
        bookingAdapter = ClientBookingAdapter(
            requireContext(), // Aqui usamos o contexto da Activity ou Fragment
            bookings,
            onConfirmClick = { booking -> confirmBooking(booking) },
            onCancelClick = { booking -> cancelBooking(booking) }
        )

        bookingRecyclerView.adapter = bookingAdapter
    }


    private fun loadBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    bookings = querySnapshot.documents.mapNotNull { document ->
                        val booking = document.toObject(Booking::class.java)
                        booking?.id = document.id // Atribui o ID do documento ao objeto Booking
                        booking
                    }.filter { isFutureBooking(it) } // Filtrar agendamentos futuros

                    if (bookings.isEmpty()) {
                        showEmptyBookingsMessage(true)
                    } else {
                        showEmptyBookingsMessage(false)
                        bookingAdapter.updateBookings(bookings)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BookingFragment", "Erro ao buscar agendamentos: ${e.message}")
                    Toast.makeText(requireContext(), "Erro ao buscar agendamentos.", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun showEmptyBookingsMessage(show: Boolean) {
        if (show) {
            emptyBookingsMessage.visibility = View.VISIBLE
            emptyBookingsImage.visibility = View.VISIBLE
            bookingRecyclerView.visibility = View.GONE
        } else {
            emptyBookingsMessage.visibility = View.GONE
            emptyBookingsImage.visibility = View.GONE
            bookingRecyclerView.visibility = View.VISIBLE
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
                set(Calendar.YEAR, bookingDateParts[2])
                set(Calendar.MONTH, bookingDateParts[1] - 1)
                set(Calendar.DAY_OF_MONTH, bookingDateParts[0])
            }
            return bookingCalendar.after(currentDate)
        }
        return false
    }

    private fun confirmBooking(booking: Booking) {
        booking.id?.let { bookingId ->
            firestore.collection("bookings").document(bookingId)
                .update("status_cliente", "confirmed")
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Agendamento confirmado.", Toast.LENGTH_SHORT).show()
                    loadBookings()
                }
                .addOnFailureListener { e ->
                    Log.e("BookingFragment", "Erro ao confirmar agendamento: ${e.message}")
                    Toast.makeText(requireContext(), "Erro ao confirmar agendamento.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun cancelBooking(booking: Booking) {
        booking.id?.let { bookingId ->
            firestore.collection("bookings").document(bookingId)
                .update("status_cliente", "canceled")
                .addOnSuccessListener {
                    // Exclui o agendamento após marcar como cancelado
                    deleteBooking(bookingId)
                }
                .addOnFailureListener { e ->
                    Log.e("BookingFragment", "Erro ao cancelar agendamento: ${e.message}")
                    Toast.makeText(requireContext(), "Erro ao cancelar agendamento.", Toast.LENGTH_SHORT).show()
                }
        }
    }


    // Função para excluir o agendamento do Firestore
    private fun deleteBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Agendamento excluído.", Toast.LENGTH_SHORT).show()
                loadBookings() // Recarrega os agendamentos após a exclusão
            }
            .addOnFailureListener { e ->
                Log.e("BookingFragment", "Erro ao excluir agendamento: ${e.message}")
                Toast.makeText(requireContext(), "Erro ao excluir agendamento.", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}
