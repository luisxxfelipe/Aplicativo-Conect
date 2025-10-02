package com.conect.aplicativoconect.ui.adapters

import android.app.AlertDialog
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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Booking
import com.conect.aplicativoconect.utils.TokenManager
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class BookingAdapter(
    private var bookings: List<Booking>,
    private val context: Context,
    private val onConfirmBooking: (String) -> Unit,
    private val onCancelBooking: (String) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    inner class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.bookingUserName)
        val date: TextView = view.findViewById(R.id.bookingDate)
        val serviceType: TextView = view.findViewById(R.id.bookingServiceType)
        val bookingTime: TextView = view.findViewById(R.id.bookingTime)
        val confirmButton: Button = view.findViewById(R.id.confirmButton)
        val completeButton: Button = view.findViewById(R.id.completeButton)
        val notCompletedButton: Button = view.findViewById(R.id.notCompletedButton)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        val userImageView: CircleImageView = view.findViewById(R.id.userImageView)
        val whatsappIcon: ImageView = itemView.findViewById(R.id.whatsappIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_booking_admin, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        holder.userName.text = booking.name
        holder.date.text = booking.date
        holder.serviceType.text = booking.serviceName
        holder.bookingTime.text = "${booking.hour}h"

        // Configuração do ícone do WhatsApp
        holder.whatsappIcon.setOnClickListener {
            Log.d("ClientBookingAdapter", "Ícone de WhatsApp clicado para ${booking.name}")
            val businessPhoneNumber = formatPhoneNumber(booking.phoneCliente)
            if (businessPhoneNumber != null) {
                val message =
                    "Olá ${booking.name}, aqui é do estabelecimento ${booking.companyName}. Queria conversar sobre seu agendamento."
                openWhatsApp(businessPhoneNumber, message)
            } else {
                Toast.makeText(context, "Número de telefone inválido", Toast.LENGTH_SHORT).show()
                Log.e("BusinessAdapter", "Número de telefone inválido: ${booking.phoneCliente}")
            }
        }

        // Carregar a imagem do usuário usando Glide
        Glide.with(context)
            .load(booking.userImageUrl)
            .placeholder(R.drawable.foto_perfil_generica)
            .error(R.drawable.foto_perfil_generica)
            .into(holder.userImageView)

        val bookingId = booking.id ?: return

        // Configura a barra de status com base no status atual do agendamento
        val statusIndicator = holder.itemView.findViewById<View>(R.id.statusIndicator)
        val statusColor = when (booking.status_adm) {
            "completed" -> ContextCompat.getColor(holder.itemView.context, R.color.green)
            "cancelled", "not_completed" -> ContextCompat.getColor(
                holder.itemView.context,
                R.color.red
            )

            else -> ContextCompat.getColor(holder.itemView.context, R.color.yellow)
        }
        statusIndicator.setBackgroundColor(statusColor)

        // Verifica se a data e hora do agendamento já passou
        val hasPassedTime = hasBookingTimePassed(booking.date, booking.hour)

        // Verificar se o agendamento está completo
        val isCompleted = booking.status_adm == "completed" || booking.status_adm == "cancelled"

        // Define a lógica de exibição dos botões
        val showConfirmationButtons = booking.status_adm == "pending" && !isCompleted
        val showCompletionButtons =
            hasPassedTime && !isCompleted && booking.status_adm == "confirmed"

        holder.confirmButton.visibility = if (showConfirmationButtons) View.VISIBLE else View.GONE
        holder.cancelButton.visibility = if (showConfirmationButtons) View.VISIBLE else View.GONE
        holder.completeButton.visibility = if (showCompletionButtons) View.VISIBLE else View.GONE
        holder.notCompletedButton.visibility =
            if (showCompletionButtons) View.VISIBLE else View.GONE


        // Ação de confirmar agendamento
        holder.confirmButton.setOnClickListener {
            showConfirmationDialog(
                "Confirmar Agendamento",
                "Tem certeza que deseja confirmar este agendamento?"
            ) {
                onConfirmBooking(bookingId)
                firestore.collection("bookings").document(bookingId)
                    .update("status_adm", "confirmed")
                    .addOnSuccessListener {
                        booking.status_adm = "confirmed"
                        notifyItemChanged(position)
                        // Notificar o cliente
                        sendNotification(
                            bookingId,
                            "Agendamento Confirmado",
                            "Olá ${booking.name}, seu agendamento foi confirmado.",
                            false
                        )
                        // Notificar o administrador
                        sendNotification(
                            bookingId,
                            "Novo Agendamento Confirmado",
                            "O agendamento foi confirmado.",
                            true
                        )
                    }
            }
        }

        // Ação de cancelar agendamento
        holder.cancelButton.setOnClickListener {
            showConfirmationDialog(
                "Cancelar Agendamento",
                "Tem certeza que deseja cancelar este agendamento?"
            ) {
                firestore.collection("bookings").document(bookingId)
                    .update("status_adm", "cancelled")
                    .addOnSuccessListener {
                        // Notificar o cliente
                        sendNotification(
                            bookingId,
                            "Agendamento Cancelado",
                            "Olá ${booking.name}, seu agendamento foi cancelado.",
                            false
                        )
                        coroutineScope.launch {
                            delay(2000L)
                            firestore.collection("bookings").document(bookingId)
                                .delete()
                                .addOnSuccessListener {
                                    val updatedBookings = bookings.toMutableList().apply {
                                        remove(booking)
                                    }
                                    updateData(updatedBookings)
                                    onCancelBooking(bookingId)
                                    notifyItemRemoved(position)
                                }
                        }
                    }
            }
        }

        // Ação de completar o serviço
        holder.completeButton.setOnClickListener {
            showConfirmationDialog(
                "Marcar como Completo",
                "Deseja marcar este agendamento como completo?"
            ) {
                firestore.collection("bookings").document(bookingId)
                    .update(mapOf("status_adm" to "completed", "status_cliente" to "completed"))
                    .addOnSuccessListener {
                        booking.status_adm = "completed"
                        booking.status_cliente = "completed"
                        notifyItemChanged(position)
                        // Notificar o cliente
                        sendNotification(
                            bookingId,
                            "Serviço Completo",
                            "O serviço para ${booking.serviceName} foi concluído! Agora você pode avaliá-lo.",
                            false
                        )
                    }
            }
        }

        // Ação de marcar como não concluído
        holder.notCompletedButton.setOnClickListener {
            showConfirmationDialog(
                "Não Completo",
                "Deseja marcar este agendamento como não completo?"
            ) {
                firestore.collection("bookings").document(bookingId)
                    .update("status_adm", "not_completed")
                    .addOnSuccessListener {
                        coroutineScope.launch {
                            delay(2000L)
                            firestore.collection("bookings").document(bookingId)
                                .delete()
                                .addOnSuccessListener {
                                    val updatedBookings = bookings.toMutableList().apply {
                                        remove(booking)
                                    }
                                    updateData(updatedBookings)
                                    onCancelBooking(bookingId)
                                    notifyItemRemoved(position)
                                }
                        }
                    }
            }
        }
    }

    // Formata o número de telefone para o padrão internacional
    private fun formatPhoneNumber(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrEmpty()) return null
        val sanitizedNumber = phoneNumber.replace("[^\\d]".toRegex(), "")
        return if (sanitizedNumber.startsWith("55")) sanitizedNumber else "55$sanitizedNumber"
    }

    // Abre o WhatsApp com o número de telefone e a mensagem
    private fun openWhatsApp(phoneNumber: String, message: String) {
        val uri = Uri.parse("https://wa.me/$phoneNumber?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Erro ao abrir WhatsApp. Certifique-se de que o aplicativo está instalado.",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("BusinessAdapter", "Erro ao abrir WhatsApp: ${e.message}")
        }
    }


    // Função para verificar se o horário do agendamento já passou
    private fun hasBookingTimePassed(date: String?, hour: String?): Boolean {
        if (date == null || hour == null) return false
        val bookingDateTime = parseDateTime(date, hour) ?: return false
        val currentDateTime = Calendar.getInstance().time
        return bookingDateTime.before(currentDateTime)
    }

    // Função auxiliar para análise de data/hora
    private fun parseDateTime(date: String, hour: String): Date? {
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

    fun updateData(newBookings: List<Booking>) {
        val diffCallback = BookingDiffCallback(bookings, newBookings)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        bookings = newBookings
        diffResult.dispatchUpdatesTo(this)
    }


    override fun getItemCount(): Int = bookings.size

    private fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Sim") { _, _ -> onConfirm() }
            .setNegativeButton("Não", null)
            .create()

        dialog.show()

        // Acessando os botões e alterando as cores
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.roxo
            )
        ) // Cor roxa para o texto do botão "Sim"

        val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        negativeButton.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.black
            )
        ) // Cor laranja para o texto do botão "Não"
    }


    // Função para enviar notificações, com a opção de notificar o cliente ou o administrador
    private fun sendNotification(
        bookingId: String,
        title: String,
        message: String,
        notifyAdmin: Boolean
    ) {
        if (notifyAdmin) {
            // Buscar o token do administrador
            firestore.collection("bookings").document(bookingId)
                .get()
                .addOnSuccessListener { bookingDocument ->
                    val companyId =
                        bookingDocument.getString("companyId") ?: return@addOnSuccessListener
                    fetchBusinessToken(companyId) { adminToken ->
                        if (adminToken != null) {
                            sendFCMNotification(adminToken, title, message)
                        } else {
                        }
                    }
                }
                .addOnFailureListener { _ ->
                }
        } else {
            // Buscar o token do usuário (cliente)
            firestore.collection("bookings").document(bookingId)
                .get()
                .addOnSuccessListener { bookingDocument ->
                    val userId = bookingDocument.getString("userId") ?: return@addOnSuccessListener
                    fetchUserToken(userId) { userToken ->
                        if (userToken != null) {
                            sendFCMNotification(userToken, title, message)
                        } else {
                        }
                    }
                }
                .addOnFailureListener { _ ->
                }
        }
    }

    // Função para buscar o token do administrador
    private fun fetchBusinessToken(companyId: String, callback: (String?) -> Unit) {
        firestore.collection("business").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcmToken")
                Log.d("FCM", "Token do administrador: $token")
                callback(token)
            }
            .addOnFailureListener { _ ->
                callback(null)
            }
    }

    // Função para buscar o token do usuário (cliente)
    private fun fetchUserToken(userId: String, callback: (String?) -> Unit) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcmToken")
                callback(token)
            }
            .addOnFailureListener { _ ->
                callback(null)
            }
    }

    // Função para enviar a notificação FCM
    private fun sendFCMNotification(token: String, title: String, message: String) {
        val url = "https://fcm.googleapis.com/v1/projects/aplicativo-conect-f253d/messages:send"
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

        coroutineScope.launch {
            val accessToken = withContext(Dispatchers.IO) {
                // TODO: Implementar obtenção de token para FCM
                "temp_token"
            }

            // Token está sempre disponível em modo temporário

            val request = object : StringRequest(
                Method.POST, url,
                Response.Listener { _ ->
                },
                Response.ErrorListener { _ ->
                }
            ) {
                override fun getHeaders(): Map<String, String> {
                    return mapOf(
                        "Authorization" to "Bearer $accessToken",
                        "Content-Type" to "application/json"
                    )
                }

                override fun getBody(): ByteArray = payload.toByteArray(Charsets.UTF_8)
            }

            withContext(Dispatchers.Main) {
                Volley.newRequestQueue(context).add(request)
            }
        }
    }
}
