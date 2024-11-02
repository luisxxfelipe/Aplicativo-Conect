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
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // Acessa o CardView diretamente para aplicar cor e elevação
        val cardView = holder.itemView as androidx.cardview.widget.CardView

        // Definir cor de fundo baseada no status do agendamento
        val backgroundColor = if (booking.status_adm == "confirmed") {
            getRandomColor(cardView)  // Aplica cor aleatória para "confirmed"
        } else {
            ContextCompat.getColor(context, R.color.white)  // Cor padrão
        }
        cardView.setCardBackgroundColor(backgroundColor)

        // Ajustar elevação baseada na visibilidade dos botões
        val showButtons = booking.status_adm == "pending"
        cardView.cardElevation = if (showButtons) 8f else 0f

        // Controla a visibilidade dos botões
        holder.confirmButton.visibility = if (showButtons) View.VISIBLE else View.GONE
        holder.cancelButton.visibility = if (showButtons) View.VISIBLE else View.GONE

        // Configura cliques nos botões de confirmação e cancelamento
        holder.confirmButton.setOnClickListener {
            showConfirmationDialog(
                "Confirmar Agendamento",
                "Tem certeza que deseja confirmar este agendamento?"
            ) {
                onConfirmBooking(bookingId)
                sendNotificationToUser(
                    bookingId,
                    "Agendamento Confirmado",
                    "O agendamento de ${booking.name} foi confirmado!"
                )
                booking.status_adm = "confirmed"  // Atualiza o status localmente
                notifyItemChanged(position)  // Atualiza a interface
            }
        }

        holder.cancelButton.setOnClickListener {
            showConfirmationDialog(
                "Cancelar Agendamento",
                "Tem certeza que deseja cancelar este agendamento?"
            ) {
                onCancelBooking(bookingId)
                sendNotificationToUser(
                    bookingId,
                    "Agendamento Cancelado",
                    "Olá ${booking.name}, seu agendamento foi cancelado."
                )
                booking.status_adm = "cancelled"  // Atualiza o status localmente
                notifyItemChanged(position)  // Atualiza a interface
            }
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
