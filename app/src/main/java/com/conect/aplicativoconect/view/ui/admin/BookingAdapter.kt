import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking

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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        holder.userName.text = booking.name
        holder.date.text = booking.date
        holder.serviceType.text = booking.serviceName
        holder.bookingTime.text = "${booking.hour}:00"

        val bookingId = booking.id ?: run {
            Log.e("BookingAdapter", "Booking ID is null for booking: $booking")
            return
        }

        if (booking.status_adm == "confirmed") {
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
