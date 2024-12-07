package com.conect.aplicativoconect.view.ui.client

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import java.util.Calendar
import java.util.Date

class ClientBookingAdapter(
    private val context: Context,
    private var bookings: List<Booking>,
    private val onConfirmClick: (Booking) -> Unit,
    private val onRateClick: (Booking) -> Unit = {},
    private val onCancelClick: (Booking) -> Unit,
    private val onEmptyList: () -> Unit
) : RecyclerView.Adapter<ClientBookingAdapter.ClientBookingViewHolder>() {

    private val colorList = listOf(
        R.color.colorCategory1,
        R.color.colorCategory2,
        R.color.colorCategory3,
        R.color.colorCategory4
    )
    private var lastColorIndex: Int? = null

    inner class ClientBookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookingUserName: TextView = itemView.findViewById(R.id.bookingUserName)
        val bookingServiceType: TextView = itemView.findViewById(R.id.bookingServiceType)
        val bookingTime: TextView = itemView.findViewById(R.id.bookingTime)
        val bookingDate: TextView = itemView.findViewById(R.id.bookingDate)
        val confirmButton: Button = itemView.findViewById(R.id.confirmButton)
        val cancelButton: Button = itemView.findViewById(R.id.cancelButton)
        val rateButton: Button = itemView.findViewById(R.id.rateButton)
        val whatsappIcon: ImageView =
            itemView.findViewById(R.id.whatsappIcon) // Novo ícone do WhatsApp

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

        // Configuração do ícone do WhatsApp
        holder.whatsappIcon.setOnClickListener {
            var businessPhoneNumber =
                booking.businessPhone // Adicione o número de telefone do Business na classe Booking

            // Limpar o número de telefone, removendo espaços, parênteses, e hífens
            if (businessPhoneNumber != null) {
                businessPhoneNumber = businessPhoneNumber.replace("[^\\d]".toRegex(), "")
            }

            // Adicionar o código do país, se necessário (por exemplo, Brasil é 55)
            if (businessPhoneNumber != null) {
                if (!businessPhoneNumber.startsWith("55")) {
                    businessPhoneNumber = "55$businessPhoneNumber"
                }
            }

            val message =
                "Olá, meu nome é ${booking.name}. Queria tirar dúvidas sobre meu agendamento de ${booking.serviceName} no dia ${booking.date} às ${booking.hour}."

            openWhatsApp(businessPhoneNumber, message)
        }


        val statusIndicator = holder.itemView.findViewById<View>(R.id.statusIndicator)
        val statusColor = if (booking.status_cliente == "confirmed") {
            getRandomColor(statusIndicator)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.yellow)
        }
        statusIndicator.setBackgroundColor(statusColor)

        val isPending = booking.status_cliente == "pending"
        val isCompleted = booking.status_cliente == "completed" && booking.status_adm == "completed"
        val hasPassedTime = hasServiceTimePassed(booking)
        val hasRating = booking.rating != null

        // Visibilidade dos botões com a nova lógica de avaliação
        holder.confirmButton.visibility = if (isPending) View.VISIBLE else View.GONE
        holder.cancelButton.visibility = if (isPending) View.VISIBLE else View.GONE
        holder.rateButton.visibility =
            if (isCompleted && hasPassedTime && !hasRating) View.VISIBLE else View.GONE

        holder.confirmButton.setOnClickListener { onConfirmClick(booking) }
        holder.cancelButton.setOnClickListener {
            onCancelClick(booking)
            removeBookingAtPosition(holder.adapterPosition)
        }
        holder.rateButton.setOnClickListener { onRateClick(booking) }
    }

    // Função para abrir o WhatsApp
    private fun openWhatsApp(phoneNumber: String?, message: String) {
        if (!phoneNumber.isNullOrEmpty()) {
            val uri = Uri.parse("https://wa.me/$phoneNumber?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.whatsapp")

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("WhatsApp", "Erro ao abrir o WhatsApp: ${e.message}")
            }
        } else {
            Log.e("WhatsApp", "Número de telefone do Business não está disponível.")
        }
    }

    private fun hasServiceTimePassed(booking: Booking): Boolean {
        val bookingDateTime = parseDateTime(booking.date, booking.hour) ?: return false
        val currentDateTime = Calendar.getInstance().time
        return bookingDateTime.before(currentDateTime)
    }

    private fun parseDateTime(date: String?, hour: String?): Date? {
        if (date == null || hour == null) return null
        return try {
            val dateParts = date.split("/").map { it.toInt() }
            val timeParts = hour.split(":").map { it.toIntOrNull() ?: 0 }
            Calendar.getInstance().apply {
                set(Calendar.YEAR, dateParts[2])
                set(Calendar.MONTH, dateParts[1] - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[0])
                set(Calendar.HOUR_OF_DAY, timeParts[0])
                set(Calendar.MINUTE, timeParts.getOrElse(1) { 0 })
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        } catch (e: Exception) {
            null
        }
    }

    private fun removeBookingAtPosition(position: Int) {
        bookings = bookings.toMutableList().apply { removeAt(position) }
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, bookings.size)
        if (bookings.isEmpty()) {
            onEmptyList()
        }
    }

    override fun getItemCount(): Int = bookings.size

    private fun getRandomColor(view: View): Int {
        var newColorIndex: Int
        do {
            newColorIndex = (colorList.indices).random()
        } while (newColorIndex == lastColorIndex)

        lastColorIndex = newColorIndex
        return ContextCompat.getColor(view.context, colorList[newColorIndex])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
        if (bookings.isEmpty()) {
            onEmptyList()
        }
    }
}
