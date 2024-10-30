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
    private val onCancelClick: (Booking) -> Unit
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
        holder.bookingTime.text = "${booking.hour}:00h"

        // Aplique a cor ao CardView diretamente
        val cardView = holder.itemView as androidx.cardview.widget.CardView
        if (booking.status_cliente == "confirmed") {
            val randomColor = getRandomColor(cardView)  // Cor aleatória
            cardView.setCardBackgroundColor(randomColor)
        }

        val showButtons = booking.status_cliente == "pending"
        holder.confirmButton.visibility = if (showButtons) View.VISIBLE else View.GONE
        holder.cancelButton.visibility = if (showButtons) View.VISIBLE else View.GONE

        cardView.cardElevation = if (showButtons) 8f else 0f  // Aplica ou remove a elevação

        holder.confirmButton.setOnClickListener { onConfirmClick(booking) }
        holder.cancelButton.setOnClickListener { onCancelClick(booking) }
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
    }
}
