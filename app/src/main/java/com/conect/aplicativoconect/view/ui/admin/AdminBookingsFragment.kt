package com.conect.aplicativoconect.view.ui.admin

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminBookingsFragment : Fragment() {

    private lateinit var emptyBookingsMessage: TextView
    private lateinit var emptyBookingsImage: ImageView
    private lateinit var bookingsRecyclerView: RecyclerView
    private val bookingRepository = BookingRepository() // Instancia do repositório de agendamentos

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_bookings, container, false)

        emptyBookingsMessage = view.findViewById(R.id.emptyBookingsMessage)
        emptyBookingsImage = view.findViewById(R.id.emptyBookingsImage)
        bookingsRecyclerView = view.findViewById(R.id.bookingsRecyclerView)

        loadBookings() // Método para carregar os agendamentos do banco de dados
        return view
    }

    private fun loadBookings() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bookings = bookingRepository.getMonthBookings() // Altere para o método de busca adequado

                withContext(Dispatchers.Main) {
                    if (bookings.isEmpty()) {
                        // Se não houver agendamentos, mostre a mensagem e a imagem
                        emptyBookingsMessage.visibility = View.VISIBLE
                        emptyBookingsImage.visibility = View.VISIBLE
                        bookingsRecyclerView.visibility = View.GONE
                    } else {
                        // Se houver agendamentos
                        emptyBookingsMessage.visibility = View.GONE
                        emptyBookingsImage.visibility = View.GONE
                        bookingsRecyclerView.visibility = View.VISIBLE

                        setupRecyclerView(bookings) // Configura o RecyclerView
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
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
        val adapter = BookingAdapter(bookings)
        bookingsRecyclerView.adapter = adapter
    }
}
