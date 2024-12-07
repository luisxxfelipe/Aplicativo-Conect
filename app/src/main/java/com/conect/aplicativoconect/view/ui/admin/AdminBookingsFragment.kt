package com.conect.aplicativoconect.view.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class AdminBookingsFragment : Fragment() {

    private lateinit var newBookingsRecyclerView: RecyclerView
    private lateinit var oldBookingsRecyclerView: RecyclerView
    private lateinit var newBookingAdapter: BookingAdapter
    private lateinit var oldBookingAdapter: BookingAdapter
    private lateinit var emptyBookingsMessage: TextView
    private lateinit var emptyBookingsImage: ImageView
    private lateinit var newBookingsTitle: TextView
    private lateinit var oldBookingsTitle: TextView
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var lastVisibleBooking: DocumentSnapshot? = null
    private val PAGE_SIZE = 20 // Quantidade de agendamentos por vez

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_bookings, container, false)

        // Inicializar as views
        newBookingsRecyclerView = view.findViewById(R.id.recyclerViewNewBookings)
        oldBookingsRecyclerView = view.findViewById(R.id.recyclerViewOldBookings)
        emptyBookingsMessage = view.findViewById(R.id.emptyBookingsMessage)
        emptyBookingsImage = view.findViewById(R.id.emptyBookingsImage)
        newBookingsTitle = view.findViewById(R.id.newBookingsTitle)
        oldBookingsTitle = view.findViewById(R.id.oldBookingsTitle)

        setupRecyclerViews()
        loadBookings()

        return view
    }

    private fun loadBookings() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obter o ID do negócio do admin
                val businessSnapshot = firestore.collection("business")
                    .whereEqualTo("ownerId", currentUserUid)
                    .get()
                    .await()

                val businessId = businessSnapshot.documents.firstOrNull()?.id

                if (businessId == null) {
                    withContext(Dispatchers.Main) { showErrorMessage("Empresa não encontrada.") }
                    return@launch
                }

                // Obter os agendamentos do negócio (com paginação)
                val query = firestore.collection("bookings")
                    .whereEqualTo("companyId", businessId)
                    .orderBy("date") // Ordena para evitar resultados desordenados
                    .limit(PAGE_SIZE.toLong())

                // Se existir um último agendamento carregado, buscamos os próximos
                lastVisibleBooking?.let {
                    query.startAfter(it)
                }

                val bookingsSnapshot = query.get().await()

                // Particiona os agendamentos em futuros e antigos
                val (newBookings, oldBookings) = bookingsSnapshot.documents.mapNotNull { document ->
                    val booking = document.toObject(Booking::class.java)
                    booking?.id = document.id
                    booking
                }.partition { isFutureBooking(it) }

                // Atualiza a referência ao último agendamento
                lastVisibleBooking = bookingsSnapshot.documents.lastOrNull()

                // Ordena os agendamentos futuros por data e hora
                val sortedNewBookings = newBookings.sortedBy { booking ->
                    parseDateTime(
                        booking.date ?: "",
                        booking.hour ?: ""
                    ) // Ordenando por data e hora
                }

                withContext(Dispatchers.Main) {
                    updateUI(sortedNewBookings, oldBookings)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorMessage("Erro ao carregar agendamentos.")
                }
            }
        }
    }


    private fun adjustRecyclerViewHeight(recyclerView: RecyclerView, adapter: BookingAdapter) {
        val itemCount = adapter.itemCount

        val params = recyclerView.layoutParams
        // Lógica para limitar a altura a 420dp para até 3 itens
        if (itemCount > 3) {
            params.height = resources.getDimensionPixelSize(R.dimen.fixed_height_for_3_items)
        } else {
            params.height = RecyclerView.LayoutParams.WRAP_CONTENT
        }
        recyclerView.layoutParams = params
    }

    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDateTime = Calendar.getInstance().time
        val bookingDateTime = booking.date?.let { parseDateTime(it, booking.hour) } ?: return false
        return bookingDateTime.after(currentDateTime)
    }

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
            Log.e("AdminBookingsFragment", "Erro ao analisar a data/hora: ${e.message}")
            null
        }
    }

    private fun updateUI(newBookings: List<Booking>, oldBookings: List<Booking>) {
        if (newBookings.isEmpty() && oldBookings.isEmpty()) {
            showEmptyMessage(true)
        } else {
            showEmptyMessage(false)

            newBookingsTitle.visibility = if (newBookings.isNotEmpty()) View.VISIBLE else View.GONE
            oldBookingsTitle.visibility = if (oldBookings.isNotEmpty()) View.VISIBLE else View.GONE

            newBookingsRecyclerView.visibility =
                if (newBookings.isNotEmpty()) View.VISIBLE else View.GONE
            oldBookingsRecyclerView.visibility =
                if (oldBookings.isNotEmpty()) View.VISIBLE else View.GONE

            // Atualize apenas os novos agendamentos
            newBookingAdapter.addBookings(newBookings)
            newBookingAdapter.notifyDataSetChanged() // Ou você pode usar notifyItemInserted() para itens novos

            oldBookingAdapter.updateData(oldBookings)

            // Ajusta a altura das RecyclerViews
            adjustRecyclerViewHeight(newBookingsRecyclerView, newBookingAdapter)
            adjustRecyclerViewHeight(oldBookingsRecyclerView, oldBookingAdapter)
        }
    }


    private fun showEmptyMessage(show: Boolean) {
        emptyBookingsMessage.visibility = if (show) View.VISIBLE else View.GONE
        emptyBookingsImage.visibility = if (show) View.VISIBLE else View.GONE
        newBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        oldBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showErrorMessage(message: String) {
        emptyBookingsMessage.text = message
        showEmptyMessage(true)
    }

    private fun setupRecyclerViews() {
        newBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
        oldBookingsRecyclerView.layoutManager = LinearLayoutManager(context)

        newBookingAdapter = BookingAdapter(
            bookings = listOf(),
            context = requireContext(),
            onConfirmBooking = { bookingId -> confirmBooking(bookingId) },
            onCancelBooking = { bookingId -> cancelBooking(bookingId) }
        )

        oldBookingAdapter = BookingAdapter(
            bookings = listOf(),
            context = requireContext(),
            onConfirmBooking = { bookingId -> confirmBooking(bookingId) },
            onCancelBooking = { bookingId -> cancelBooking(bookingId) }
        )

        newBookingsRecyclerView.adapter = newBookingAdapter
        oldBookingsRecyclerView.adapter = oldBookingAdapter
    }

    private fun confirmBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .update("status_adm", "confirmed")
            .addOnSuccessListener {
                Log.d("AdminBookingsFragment", "Agendamento confirmado com sucesso!")
                loadBookings() // Recarrega os agendamentos
            }
            .addOnFailureListener { e ->
                Log.w("AdminBookingsFragment", "Erro ao confirmar agendamento", e)
            }
    }

    private fun cancelBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .update("status_adm", "cancelled") // Apenas atualiza o status para 'cancelled'
            .addOnSuccessListener {
                Log.d("AdminBookingsFragment", "Status do agendamento atualizado para 'cancelled'.")
                loadBookings() // Atualiza a lista de agendamentos para refletir o novo status
            }
            .addOnFailureListener { e ->
                Log.w("AdminBookingsFragment", "Erro ao atualizar status para 'cancelled': ", e)
            }
    }
}
