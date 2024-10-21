package com.conect.aplicativoconect.view.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking

class AdminBookingsFragment : Fragment() {

    private lateinit var emptyBookingsMessage: TextView
    private lateinit var emptyBookingsImage: ImageView
    private var bookings: List<Booking> = listOf() // Carregar agendamentos de um banco de dados

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_bookings, container, false)

        emptyBookingsMessage = view.findViewById(R.id.emptyBookingsMessage)
        emptyBookingsImage = view.findViewById(R.id.emptyBookingsImage)

        loadBookings() // Método para carregar os agendamentos do banco de dados
        return view
    }

    private fun loadBookings() {
        // Aqui você deve buscar os agendamentos do banco de dados e atualizar a lista
        // Exemplo: bookings = obterAgendamentosDoBancoDeDados()

        if (bookings.isEmpty()) {
            // Se não houver agendamentos, mostre a mensagem e a imagem
            emptyBookingsMessage.visibility = View.VISIBLE
            emptyBookingsImage.visibility = View.VISIBLE
        } else {
            // Se houver agendamentos, você pode implementar outra lógica aqui
            emptyBookingsMessage.visibility = View.GONE
            emptyBookingsImage.visibility = View.GONE
            // Adicione aqui a lógica para exibir os agendamentos se necessário
        }
    }
}
