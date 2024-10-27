package com.conect.aplicativoconect.view.ui.client

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import com.android.volley.Response
import com.google.auth.oauth2.GoogleCredentials
import java.io.FileNotFoundException
import java.io.IOException

class ClientBookingAdapter(
    private val context: Context,
    private var bookings: List<Booking>,
    private val onConfirmClick: (Booking) -> Unit,
    private val onCancelClick: (Booking) -> Unit
) : RecyclerView.Adapter<ClientBookingAdapter.ClientBookingViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private var isDialogShowing = false

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

        holder.confirmButton.visibility =
            if (booking.status_cliente == "pending") View.VISIBLE else View.GONE
        holder.cancelButton.visibility =
            if (booking.status_cliente == "pending") View.VISIBLE else View.GONE

        holder.confirmButton.setOnClickListener {
            showConfirmationDialog(
                "Confirmar Agendamento",
                "Tem certeza que deseja confirmar este agendamento?",
                onConfirm = {
                    onConfirmClick(booking)
                    sendNotificationToBusiness(
                        booking,
                        "Agendamento Confirmado",
                        "Seu agendamento foi confirmado."
                    )
                    resetDialogState()
                }
            )
        }

        holder.cancelButton.setOnClickListener {
            showConfirmationDialog(
                "Cancelar Agendamento",
                "Tem certeza que deseja cancelar este agendamento?",
                onConfirm = {
                    onCancelClick(booking)
                    sendNotificationToBusiness(
                        booking,
                        "Agendamento Cancelado",
                        "Seu agendamento foi cancelado."
                    )
                    resetDialogState()
                }
            )
        }
    }

    private fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        if (isDialogShowing) return

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Sim", null)
            .setNegativeButton("Não", null)
            .setOnDismissListener { resetDialogState() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                onConfirm()
                dialog.dismiss()
            }
        }

        isDialogShowing = true
        dialog.show()
    }

    private fun resetDialogState() {
        isDialogShowing = false
    }

    private fun sendNotificationToBusiness(booking: Booking, title: String, message: String) {
        val companyId = booking.companyId ?: return

        firestore.collection("business").document(companyId)
            .get()
            .addOnSuccessListener { businessDoc ->
                val fcmToken = businessDoc.getString("fcmToken")
                if (!fcmToken.isNullOrEmpty()) {
                    sendFCMNotification(fcmToken, title, message)
                } else {
                    Log.e("FCM", "Token FCM da empresa não encontrado para o ID $companyId.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Erro ao buscar token da empresa: ${e.message}")
            }
    }

    private fun sendFCMNotification(token: String, title: String, message: String) {
        val url = "https://fcm.googleapis.com/v1/projects/<SEU_PROJECT_ID>/messages:send"
        val payload = """
        {
          "message": {
            "token": "$token",
            "notification": {
              "title": "$title",
              "body": "$message"
            },
            "android": {
              "priority": "high"
            }
          }
        }
        """.trimIndent()

        val accessToken = getAccessTokenFromServiceAccount() ?: return

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                Log.d("FCM", "Notificação enviada com sucesso: $response")
            },
            Response.ErrorListener { error ->
                Log.e("FCM", "Erro ao enviar notificação: ${error.message}")
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer $accessToken"
                )
            }

            override fun getBody(): ByteArray = payload.toByteArray(Charsets.UTF_8)
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun getAccessTokenFromServiceAccount(): String? {
        return try {
            val inputStream =
                context.assets.open("aplicativo-conect-f253d-firebase-adminsdk-xcfgw-9dc183006f.json")
            val googleCredentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
            googleCredentials.refreshIfExpired()
            googleCredentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.e("FCM", "Erro ao carregar token: ${e.message}")
            null
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}
