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
        val bookingService: TextView = itemView.findViewById(R.id.bookingService) // Novo ID
        val bookingDateTime: TextView = itemView.findViewById(R.id.bookingDateTime) // Novo ID
        val bookingStatus: TextView = itemView.findViewById(R.id.bookingStatus) // Novo ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bookingService.text = booking.service // Ajuste conforme seu modelo Booking
        holder.bookingDateTime.text = "${booking.date} ${booking.time}" // Ajuste conforme seu modelo Booking
        holder.bookingStatus.visibility = View.GONE // Como o status está oculto, pode deixar assim
    }

    override fun getItemCount(): Int = bookings.size
}
