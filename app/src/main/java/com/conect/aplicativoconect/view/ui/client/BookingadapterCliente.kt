package com.conect.aplicativoconect.view.ui.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking

class ClientBookingAdapter(
    private var bookings: List<Booking>,
    private val onCancelClick: (Booking) -> Unit // Renomeado corretamente
) : RecyclerView.Adapter<ClientBookingAdapter.ClientBookingViewHolder>() {

    class ClientBookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookingService: TextView = itemView.findViewById(R.id.bookingService)
        val bookingDateTime: TextView = itemView.findViewById(R.id.bookingDateTime)
        val cancelButton: Button = itemView.findViewById(R.id.cancelButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientBookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cliente_booking, parent, false)
        return ClientBookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClientBookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bookingService.text = booking.serviceName
        holder.bookingDateTime.text = "${booking.date} ${booking.hour}:00h"

        holder.cancelButton.setOnClickListener {
            onCancelClick(booking) // Chama a ação de cancelamento corretamente
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        this.bookings = newBookings
        notifyDataSetChanged() // Notifica que a lista mudou
    }
}
