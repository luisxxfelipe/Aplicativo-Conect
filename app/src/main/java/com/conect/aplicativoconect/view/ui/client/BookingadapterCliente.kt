package com.conect.aplicativoconect.view.ui.client

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
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
    private val onCardClick: (String) -> Unit,
    private val onEmptyList: () -> Unit
) : RecyclerView.Adapter<ClientBookingAdapter.ClientBookingViewHolder>() {


    inner class ClientBookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookingNameTextView: TextView = itemView.findViewById(R.id.companyName)
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
        holder.bookingNameTextView.text = booking.companyName
        holder.bookingServiceType.text = booking.serviceName
        holder.bookingDate.text = booking.date
        holder.bookingTime.text = "${booking.hour}h"


        // Configuração do clique no card para abrir detalhes
        holder.itemView.setOnClickListener {
            booking.companyId?.let { companyId -> // Certifique-se de que o `companyId` está presente
                onCardClick(companyId) // Chama a função de clique com o ID da empresa
            }
        }

        // Configuração do ícone do WhatsApp
        holder.whatsappIcon.setOnClickListener {
            var businessPhoneNumber = booking.businessPhone

            if (businessPhoneNumber != null) {
                businessPhoneNumber = businessPhoneNumber.replace("[^\\d]".toRegex(), "")
                if (!businessPhoneNumber.startsWith("55")) {
                    businessPhoneNumber = "55$businessPhoneNumber"
                }
            }

            val message =
                "Olá, meu nome é ${booking.name}. Queria tirar dúvidas sobre meu agendamento de ${booking.serviceName} no dia ${booking.date} às ${booking.hour}."
            openWhatsApp(businessPhoneNumber, message)
        }

        val statusIndicator = holder.itemView.findViewById<View>(R.id.statusIndicator)
        val isCompleted = booking.status_cliente == "completed" && booking.status_adm == "completed"
        val hasRating = booking.rating != null

        val statusColor = if (isCompleted && hasRating) {
            ContextCompat.getColor(holder.itemView.context, R.color.green) // Verde para avaliado
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.yellow) // Amarelo para pendente
        }
        statusIndicator.setBackgroundColor(statusColor)

        val isPending = booking.status_cliente == "pending"
        val hasPassedTime = hasServiceTimePassed(booking)

        // Visibilidade dos botões com a lógica ajustada
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
            }
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

        // Certifique-se de verificar a lista após a remoção
        if (bookings.isEmpty()) {
            onEmptyList()
        }
    }


    fun updateBookings(newBookings: List<Booking>) {
        this.bookings = newBookings
        notifyDataSetChanged()

        // Atualiza a mensagem de lista vazia
        if (bookings.isEmpty()) {
            onEmptyList()
        }
    }

    override fun getItemCount(): Int = bookings.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()

        // Verifique e chame onEmptyList se não houver agendamentos
        if (bookings.isEmpty()) {
            onEmptyList()
        }
    }

}
