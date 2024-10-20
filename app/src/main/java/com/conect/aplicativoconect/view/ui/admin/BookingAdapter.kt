package com.conect.aplicativoconect.view.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking

class BookingAdapter(private val bookings: List<Booking>) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookingDetails: TextView = itemView.findViewById(R.id.bookingDetails) // Ajuste conforme seu layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false) // Layout para cada item
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bookingDetails.text = "${booking.service} - ${booking.date} ${booking.time}" // Exemplo de exibição
    }

    override fun getItemCount(): Int = bookings.size
}
