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
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.TokenUtils
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import com.android.volley.Response
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookingAdapter(
    private val bookings: List<Booking>,
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
        holder.bookingTime.text = "${booking.hour}:00"

        // Carregar a imagem do usuário usando Glide
        Glide.with(context)
            .load(booking.userImageUrl)
            .placeholder(R.drawable.foto_perfil_generica)
            .error(R.drawable.foto_perfil_generica)
            .into(holder.userImageView)

        val bookingId = booking.id ?: run {
            Log.e("BookingAdapter", "Booking ID is null for booking: $booking")
            return
        }

        // Esconder os botões se o status já foi confirmado ou cancelado
        if (booking.status_adm == "confirmed" || booking.status_adm == "cancelled") {
            holder.confirmButton.visibility = View.GONE
            holder.cancelButton.visibility = View.GONE
        } else {
            holder.confirmButton.visibility = View.VISIBLE
            holder.cancelButton.visibility = View.VISIBLE

            holder.confirmButton.setOnClickListener {
                showConfirmationDialog(
                    "Confirmar Agendamento",
                    "Tem certeza que deseja confirmar este agendamento?",
                    onConfirm = {
                        onConfirmBooking(bookingId)
                        sendNotificationToUser(
                            bookingId,
                            "Agendamento Confirmado",
                            "Olá ${booking.name}, seu agendamento foi confirmado!"
                        )
                        holder.confirmButton.visibility = View.GONE
                        holder.cancelButton.visibility = View.GONE
                    }
                )
            }

            holder.cancelButton.setOnClickListener {
                showConfirmationDialog(
                    "Cancelar Agendamento",
                    "Tem certeza que deseja cancelar este agendamento?",
                    onConfirm = {
                        onCancelBooking(bookingId)
                        sendNotificationToUser(
                            bookingId,
                            "Agendamento Cancelado",
                            "Olá ${booking.name}, seu agendamento foi cancelado."
                        )
                        holder.confirmButton.visibility = View.GONE
                        holder.cancelButton.visibility = View.GONE
                    }
                )
            }

        }
    }


    override fun getItemCount(): Int = bookings.size

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
