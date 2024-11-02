package com.conect.aplicativoconect.view.ui.client


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking

class ClientBookingAdapter(
    private var bookings: List<Booking>,
    private val onConfirmClick: (Booking) -> Unit,
    private val onCancelClick: (Booking) -> Unit,
    private val onEmptyList: () -> Unit // Callback para lista vazia
) : RecyclerView.Adapter<ClientBookingAdapter.ClientBookingViewHolder>() {

    // Lista de cores disponíveis
    private val colorList = listOf(
        R.color.colorCategory1,
        R.color.colorCategory2,
        R.color.colorCategory3,
        R.color.colorCategory4
    )

    private var lastColorIndex: Int? = null  // Para evitar repetição consecutiva

    inner class ClientBookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookingUserName: TextView = itemView.findViewById(R.id.bookingUserName)
        val bookingServiceType: TextView = itemView.findViewById(R.id.bookingServiceType)
        val bookingTime: TextView = itemView.findViewById(R.id.bookingTime)
        val bookingDate: TextView = itemView.findViewById(R.id.bookingDate)
        val confirmButton: Button = itemView.findViewById(R.id.confirmButton)
        val cancelButton: Button = itemView.findViewById(R.id.cancelButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientBookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_booking, parent, false)
        return ClientBookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClientBookingViewHolder, position: Int) {
        val booking = bookings[position]

        holder.bookingUserName.text = booking.name
        holder.bookingServiceType.text = booking.serviceName
        holder.bookingDate.text = booking.date
        holder.bookingTime.text = "${booking.hour}h"

        // Define a barra lateral de cor para indicar o status
        val statusIndicator = holder.itemView.findViewById<View>(R.id.statusIndicator)
        val statusColor = if (booking.status_cliente == "confirmed") {
            getRandomColor(statusIndicator)  // Cor aleatória para confirmado
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.yellow)  // Cor amarela para pendente
        }
        statusIndicator.setBackgroundColor(statusColor)

        // Mostra ou esconde os botões dependendo do status
        val showButtons = booking.status_cliente == "pending"
        holder.confirmButton.visibility = if (showButtons) View.VISIBLE else View.GONE
        holder.cancelButton.visibility = if (showButtons) View.VISIBLE else View.GONE

        holder.confirmButton.setOnClickListener { onConfirmClick(booking) }
        holder.cancelButton.setOnClickListener {
            onCancelClick(booking)
            removeBookingAtPosition(holder.adapterPosition)
        }
    }

    // Função para remover um agendamento da lista local e atualizar o RecyclerView
    private fun removeBookingAtPosition(position: Int) {
        bookings = bookings.toMutableList().apply {
            removeAt(position)
        }
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, bookings.size) // Atualiza as posições dos itens restantes

        if (bookings.isEmpty()) {
            onEmptyList() // Chama o callback quando a lista estiver vazia
        }
    }


    override fun getItemCount(): Int = bookings.size

    // Função para selecionar uma cor aleatória, evitando repetição consecutiva
    private fun getRandomColor(view: View): Int {
        var newColorIndex: Int
        do {
            newColorIndex = (colorList.indices).random()  // Gera um índice aleatório
        } while (newColorIndex == lastColorIndex)  // Evita repetir a cor consecutivamente

        lastColorIndex = newColorIndex  // Armazena o índice atual para evitar repetições
        return ContextCompat.getColor(view.context, colorList[newColorIndex])  // Retorna a cor
    }

    fun updateData(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()  // Garante que a UI seja atualizada

        if (bookings.isEmpty()) {
            onEmptyList() // Notifica se a lista está vazia após a atualização
        }
    }
}
