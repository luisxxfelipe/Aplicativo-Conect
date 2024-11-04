package com.conect.aplicativoconect.view.ui.client

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date

class ClientBookingAdapter(
    private val context: Context,
    private var bookings: List<Booking>,
    private val onConfirmClick: (Booking) -> Unit,
    private val onRateClick: (Booking) -> Unit,
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

        val statusIndicator = holder.itemView.findViewById<View>(R.id.statusIndicator)
        val statusColor = if (booking.status_cliente == "confirmed") {
            getRandomColor(statusIndicator)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.yellow)
        }
        statusIndicator.setBackgroundColor(statusColor)

        val isPending = booking.status_cliente == "pending"
        val isConfirmed = booking.status_cliente == "confirmed" && booking.status_adm == "confirmed"
        val hasPassedTime = hasServiceTimePassed(booking)
        val hasRating = booking.rating != null // Verifica se a avaliação já existe

        holder.confirmButton.visibility = if (isPending) View.VISIBLE else View.GONE
        holder.cancelButton.visibility = if (isPending) View.VISIBLE else View.GONE

        // Mostra o botão de avaliação apenas se o booking está confirmado, o horário já passou e ainda não há avaliação
        holder.rateButton.visibility = if (isConfirmed && hasPassedTime && !hasRating) View.VISIBLE else View.GONE

        holder.confirmButton.setOnClickListener { onConfirmClick(booking) }
        holder.cancelButton.setOnClickListener {
            onCancelClick(booking)
            removeBookingAtPosition(holder.adapterPosition)
        }
        holder.rateButton.setOnClickListener { showRatingPopup(booking) }
    }


    private fun showRatingPopup(booking: Booking) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.popup_rating, null)
        val ratingQuality = dialogView.findViewById<RatingBar>(R.id.ratingQuality)
        val ratingPunctuality = dialogView.findViewById<RatingBar>(R.id.ratingPunctuality)
        val ratingService = dialogView.findViewById<RatingBar>(R.id.ratingService)
        val commentEditText = dialogView.findViewById<EditText>(R.id.commentEditText) // Campo de comentário
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        saveButton.setOnClickListener {
            val qualityRating = ratingQuality.rating.toInt()
            val punctualityRating = ratingPunctuality.rating.toInt()
            val serviceRating = ratingService.rating.toInt()
            val comment = commentEditText.text.toString().takeIf { it.isNotBlank() } // Pega o comentário se não estiver vazio

            saveRatings(booking.id, qualityRating, punctualityRating, serviceRating, comment)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun saveRatings(bookingId: String?, quality: Int, punctuality: Int, service: Int, comment: String?) {
        if (bookingId == null) return

        // Inclui o comentário no mapa de dados, mesmo que esteja vazio
        val ratingData = mutableMapOf<String, Any>(
            "quality" to quality,
            "punctuality" to punctuality,
            "service" to service,
            "timestamp" to System.currentTimeMillis(),
            "comment" to (comment ?: "")  // Garante que "comment" seja uma string, mesmo que vazia
        )

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("bookings").document(bookingId)
            .update("rating", ratingData)
            .addOnSuccessListener {
                Toast.makeText(context, "Avaliação salva com sucesso!", Toast.LENGTH_SHORT).show()

                // Atualiza o booking localmente para refletir a mudança
                bookings.find { it.id == bookingId }?.let { booking ->
                    booking.rating = ratingData // Atualiza a propriedade rating localmente
                    notifyDataSetChanged() // Atualiza a lista para esconder o botão de avaliação

                    // Busca o ID da empresa e atualiza a média de avaliação
                    val companyId = booking.companyId
                    if (companyId != null) {
                        updateBusinessRating(companyId, (quality + punctuality + service) / 3.0)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClientBookingAdapter", "Erro ao salvar avaliação: ${e.message}")
                Toast.makeText(context, "Erro ao salvar avaliação", Toast.LENGTH_SHORT).show()
            }
    }


    private fun updateBusinessRating(companyId: String, newRating: Double) {
        val businessRef = FirebaseFirestore.getInstance().collection("business").document(companyId)
        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(businessRef)
            val currentRating = snapshot.getDouble("averageRating") ?: 0.0
            val ratingCount = snapshot.getLong("ratingCount")?.toInt() ?: 0

            // Calcula a nova média
            val updatedRatingCount = ratingCount + 1
            val updatedAverageRating = (currentRating * ratingCount + newRating) / updatedRatingCount

            transaction.update(businessRef, "averageRating", updatedAverageRating)
            transaction.update(businessRef, "ratingCount", updatedRatingCount)
        }.addOnSuccessListener {
            Log.d("ClientBookingAdapter", "Média de avaliação da empresa atualizada com sucesso.")
        }.addOnFailureListener { e ->
            Log.e("ClientBookingAdapter", "Erro ao atualizar média de avaliação: ${e.message}")
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

    fun updateData(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
        if (bookings.isEmpty()) {
            onEmptyList()
        }
    }
}
