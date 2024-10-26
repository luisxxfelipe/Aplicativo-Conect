import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import de.hdodenhof.circleimageview.CircleImageView

class BookingAdapter(
    private val bookings: List<Booking>,
    private val context: Context,
    private val onConfirmBooking: (String) -> Unit,
    private val onCancelBooking: (String) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

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

        // Preenchendo os campos de texto com os dados do agendamento
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

        // Verifica se o status é "confirmed" e oculta os botões se necessário
        if (booking.status_cliente == "confirmed" || booking.status_adm == "confirmed") {
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

                        // Atualiza o status do cliente localmente e oculta os botões
                        booking.status_cliente = "confirmed"
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
                    }
                )
            }
        }
    }

    override fun getItemCount(): Int = bookings.size

    // Função para exibir o diálogo de confirmação
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
}
