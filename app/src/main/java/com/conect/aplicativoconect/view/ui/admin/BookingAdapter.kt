package com.conect.aplicativoconect.view.ui.admin

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking

class BookingAdapter(private val bookings: List<Booking>) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.bookingUserName)
        val date: TextView = view.findViewById(R.id.bookingDate)
        val serviceType: TextView = view.findViewById(R.id.bookingServiceType)
        val bookingTime: TextView = view.findViewById(R.id.bookingTime)
        val confirmButton: Button = view.findViewById(R.id.confirmButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.date.text = booking.date
        holder.userName.text = booking.name
        holder.serviceType.text = booking.serviceName
        holder.bookingTime.text = "${booking.hour}:00"

        // Configurar o botão de confirmação
        holder.confirmButton.setOnClickListener {
        }
    }

    override fun getItemCount(): Int {
        return bookings.size
    }
}
