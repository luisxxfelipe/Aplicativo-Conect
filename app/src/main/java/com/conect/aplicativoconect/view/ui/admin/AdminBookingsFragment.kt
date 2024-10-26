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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminBookingsFragment : Fragment() {

    private lateinit var emptyBookingsMessage: TextView
    private lateinit var emptyBookingsImage: ImageView
    private lateinit var bookingsRecyclerView: RecyclerView
    private val bookingRepository = BookingRepository()
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_bookings, container, false)

        emptyBookingsMessage = view.findViewById(R.id.emptyBookingsMessage)
        emptyBookingsImage = view.findViewById(R.id.emptyBookingsImage)
        bookingsRecyclerView = view.findViewById(R.id.bookingsRecyclerView)

        firestore = FirebaseFirestore.getInstance()

        loadBookings()
        return view
    }

    private fun loadBookings() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Busca apenas agendamentos que não estão cancelados
                val bookings = bookingRepository.getAllBookings().filter { it.status_adm != "canceled" }

                withContext(Dispatchers.Main) {
                    if (bookings.isEmpty()) {
                        // Exibe mensagem de vazio
                        emptyBookingsMessage.visibility = View.VISIBLE
                        emptyBookingsImage.visibility = View.VISIBLE
                        bookingsRecyclerView.visibility = View.GONE
                    } else {
                        // Esconde a mensagem de vazio e configura o RecyclerView
                        emptyBookingsMessage.visibility = View.GONE
                        emptyBookingsImage.visibility = View.GONE
                        bookingsRecyclerView.visibility = View.VISIBLE
                        setupRecyclerView(bookings)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Exibe mensagem de erro
                    emptyBookingsMessage.text = "Erro ao carregar agendamentos."
                    emptyBookingsMessage.visibility = View.VISIBLE
                    emptyBookingsImage.visibility = View.VISIBLE
                    bookingsRecyclerView.visibility = View.GONE
                    Log.e("AdminBookingsFragment", "Erro ao carregar agendamentos: ", e)
                }
            }
        }
    }

    private fun setupRecyclerView(bookings: List<Booking>) {
        bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Configura o adapter com a lista de bookings
        bookingsRecyclerView.adapter = BookingAdapter(
            bookings,
            requireContext(),
            { bookingId -> confirmBooking(bookingId) },
            { bookingId -> cancelBooking(bookingId) }
        )
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
