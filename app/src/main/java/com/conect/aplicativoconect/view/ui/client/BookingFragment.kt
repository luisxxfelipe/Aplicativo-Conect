package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import android.util.Log
import android.widget.Toast
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        userId?.let { id ->
            firestore.collection("bookings")
                .whereEqualTo("userId", id) // Busca os agendamentos pelo ID do usuário
                .get()
                .addOnSuccessListener { querySnapshot ->
                    bookings = querySnapshot.documents.mapNotNull {
                        it.toObject(Booking::class.java)
                    }

                    if (bookings.isEmpty()) {
                        emptyBookingsMessage.visibility = View.VISIBLE
                        emptyBookingsImage.visibility = View.VISIBLE
                        bookingRecyclerView.visibility = View.GONE // Oculta o RecyclerView
                    } else {
                        emptyBookingsMessage.visibility = View.GONE
                        emptyBookingsImage.visibility = View.GONE
                        bookingRecyclerView.visibility = View.VISIBLE
                        bookingAdapter.updateBookings(bookings) // Atualiza a lista no adapter
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BookingFragment", "Erro ao buscar agendamentos: ${e.message}")
                    Toast.makeText(requireContext(), "Erro ao buscar agendamentos.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun cancelBooking(booking: Booking) {
        // Implementar a lógica de cancelamento de agendamento
    }
}
