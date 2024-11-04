import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.TokenUtils
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.internal.Util.parseDateTime
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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

    // Lista de cores disponíveis
    private val colorList = listOf(
        R.color.colorCategory1,
        R.color.colorCategory2,
        R.color.colorCategory3,
        R.color.colorCategory4
    )

    private var lastColorIndex: Int? = null  // Para evitar repetição consecutiva

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
            "confirmed" -> getRandomColor(statusIndicator) // Cor de confirmados
            "completed" -> ContextCompat.getColor(holder.itemView.context, R.color.green)
            "cancelled", "not_completed" -> ContextCompat.getColor(holder.itemView.context, R.color.red)
            else -> ContextCompat.getColor(holder.itemView.context, R.color.yellow)
        }
        statusIndicator.setBackgroundColor(statusColor)

        // Verifica se a data e hora do agendamento já passou
        val hasPassedTime = hasBookingTimePassed(booking.date, booking.hour)

        // Define a lógica de exibição dos botões
        val showConfirmationButtons = booking.status_adm == "pending" && !hasPassedTime
        val showCompletionButtons = hasPassedTime && booking.status_cliente == "confirmed" && booking.status_adm == "confirmed"

        holder.confirmButton.visibility = if (showConfirmationButtons) View.VISIBLE else View.GONE
        holder.cancelButton.visibility = if (showConfirmationButtons) View.VISIBLE else View.GONE
        holder.completeButton.visibility = if (showCompletionButtons) View.VISIBLE else View.GONE
        holder.notCompletedButton.visibility = if (showCompletionButtons) View.VISIBLE else View.GONE

        // Ação de confirmar agendamento
        holder.confirmButton.setOnClickListener {
            showConfirmationDialog("Confirmar Agendamento", "Tem certeza que deseja confirmar este agendamento?") {
                onConfirmBooking(bookingId)
                firestore.collection("bookings").document(bookingId)
                    .update("status_adm", "confirmed")
                    .addOnSuccessListener {
                        booking.status_adm = "confirmed"
                        notifyItemChanged(position)
                        sendNotificationToUser(
                            bookingId,
                            "Agendamento Confirmado",
                            "O agendamento de ${booking.name} foi confirmado!"
                        )
                    }
            }
        }

        // Ação de cancelar agendamento
        holder.cancelButton.setOnClickListener {
            showConfirmationDialog("Cancelar Agendamento", "Tem certeza que deseja cancelar este agendamento?") {
                firestore.collection("bookings").document(bookingId)
                    .update("status_adm", "cancelled")
                    .addOnSuccessListener {
                        sendNotificationToUser(
                            bookingId,
                            "Agendamento Cancelado",
                            "Olá ${booking.name}, seu agendamento foi cancelado."
                        )
                        GlobalScope.launch {
                            delay(2000L)
                            firestore.collection("bookings").document(bookingId)
                                .delete()
                                .addOnSuccessListener {
                                    val updatedBookings = bookings.toMutableList().apply {
                                        remove(booking)
                                    }
                                    updateData(updatedBookings)
                                    onCancelBooking(bookingId)
                                }
                        }
                    }
            }
        }

        // Ação de completar o serviço
        holder.completeButton.setOnClickListener {
            showConfirmationDialog("Marcar como Completo", "Deseja marcar este agendamento como completo?") {
                firestore.collection("bookings").document(bookingId)
                    .update("status_adm", "completed")
                    .addOnSuccessListener {
                        booking.status_adm = "completed"
                        notifyItemChanged(position)
                        sendNotificationToUser(
                            bookingId,
                            "Serviço Completo",
                            "O serviço para ${booking.serviceName} foi concluído! Agora você pode avaliá-lo."
                        )
                    }
            }
        }

        // Ação de marcar como não concluído
        holder.notCompletedButton.setOnClickListener {
            showConfirmationDialog("Não Completo", "Deseja marcar este agendamento como não completo?") {
                firestore.collection("bookings").document(bookingId)
                    .update("status_adm", "not_completed")
                    .addOnSuccessListener {
                        GlobalScope.launch {
                            delay(2000L)
                            firestore.collection("bookings").document(bookingId)
                                .delete()
                                .addOnSuccessListener {
                                    val updatedBookings = bookings.toMutableList().apply {
                                        remove(booking)
                                    }
                                    updateData(updatedBookings)
                                    onCancelBooking(bookingId)
                                }
                        }
                    }
            }
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
        bookings = newBookings
        notifyDataSetChanged()
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

    private fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Sim") { _, _ -> onConfirm() }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun sendNotificationToUser(bookingId: String, title: String, message: String) {
        firestore.collection("bookings").document(bookingId)
            .get()
            .addOnSuccessListener { document ->
                val userId = document.getString("userId") ?: return@addOnSuccessListener
                fetchUserToken(userId) { token ->
                    if (token != null) {
                        sendFCMNotification(token, title, message)
                    } else {
                        Log.e("FCM", "Token do usuário não encontrado para o ID: $userId")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Erro ao buscar agendamento: ${e.message}")
            }
    }


    private fun fetchUserToken(userId: String, callback: (String?) -> Unit) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcmToken")
                Log.d("FCM", "Token atual: $token")
                callback(token)
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Erro ao buscar token do usuário: ${e.message}")
                callback(null)
            }
    }

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
                TokenUtils.getAccessTokenFromServiceAccount(context)
            }

            if (accessToken == null) {
                Log.e("FCM", "Falha ao obter o token de acesso.")
                return@launch
            }

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
