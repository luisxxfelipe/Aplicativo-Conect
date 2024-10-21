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
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking

class BookingFragment : Fragment() {

    private lateinit var bookingRecyclerView: RecyclerView
    private lateinit var bookingAdapter: ClientBookingAdapter
    private lateinit var emptyBookingsMessage: TextView
    private lateinit var emptyBookingsImage: ImageView
    private var bookings: List<Booking> = listOf() // Carregar agendamentos do banco de dados

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cliente_booking, container, false)

        bookingRecyclerView = view.findViewById(R.id.recyclerViewClientBookings)
        emptyBookingsMessage = view.findViewById(R.id.emptyBookingsMessage)
        emptyBookingsImage = view.findViewById(R.id.emptyBookingsImage)

        setupRecyclerView()
        loadBookings() // Método para carregar os agendamentos do banco de dados

        return view
    }

    private fun setupRecyclerView() {
        bookingRecyclerView.layoutManager = LinearLayoutManager(context)
        bookingAdapter = ClientBookingAdapter(bookings) { booking ->
            // Lógica de cancelamento do agendamento
            cancelBooking(booking)
        }
        bookingRecyclerView.adapter = bookingAdapter
    }

    private fun loadBookings() {
        // Aqui você deve buscar os agendamentos do banco de dados e atualizar a lista
        // Exemplo: bookings = obterAgendamentosDoBancoDeDados()

        if (bookings.isEmpty()) {
            // Se não houver agendamentos, mostre a mensagem e a imagem
            emptyBookingsMessage.visibility = View.VISIBLE
            emptyBookingsImage.visibility = View.VISIBLE
            bookingRecyclerView.visibility = View.GONE // Oculta o RecyclerView
        } else {
            // Se houver agendamentos, atualize a lista
            emptyBookingsMessage.visibility = View.GONE
            emptyBookingsImage.visibility = View.GONE
            bookingRecyclerView.visibility = View.VISIBLE // Exibe o RecyclerView
            bookingAdapter.notifyDataSetChanged() // Notificar o adapter que os dados mudaram
        }
    }

    private fun cancelBooking(booking: Booking) {
        // Implementar a lógica de cancelamento aqui
    }
}
