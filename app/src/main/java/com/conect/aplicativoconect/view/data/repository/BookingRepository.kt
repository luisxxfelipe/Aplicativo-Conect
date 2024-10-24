package com.conect.aplicativoconect.view.data.repository

import android.util.Log
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class BookingRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // Obtenha os agendamentos de hoje
    suspend fun getTodayBookings(): List<Booking> {
        return firestore.collection("bookings")
            .whereEqualTo("date", getTodayDate())
            .get()
            .await()
            .documents
            .map { it.toObject(Booking::class.java)!! }
    }

    suspend fun getWeeklyBookings(): List<Booking> {
        val bookings = mutableListOf<Booking>()
        val calendar = Calendar.getInstance()
        val today = Date()
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val todayDateString = formatter.format(today)
        val todayDate = formatter.parse(todayDateString)

        calendar.time = todayDate
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startOfWeek = calendar.time

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        val endOfWeek = calendar.time

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val snapshot = firestore.collection("bookings")
            .whereGreaterThanOrEqualTo("date", dateFormatter.format(startOfWeek))
            .whereLessThanOrEqualTo("date", dateFormatter.format(endOfWeek))
            .get()
            .await()

        for (document in snapshot.documents) {
            val booking = document.toObject(Booking::class.java)
            booking?.let {
                bookings.add(it)
                Log.d("BookingRepository", "Booking loaded: $it") // Log para verificar dados
            }
        }

        Log.d("BookingRepository", "Total bookings this week: ${bookings.size}") // Log total de agendamentos
        return bookings
    }


    // Obtenha os agendamentos do mês
    suspend fun getMonthBookings(): List<Booking> {
        return firestore.collection("bookings")
            .whereGreaterThan("date", getFirstDateOfMonth())
            .get()
            .await()
            .documents
            .map { it.toObject(Booking::class.java)!! }
    }

    // Lucro de hoje
    suspend fun getTodayProfit(): Double {
        val todayBookings = getTodayBookings()
        return todayBookings.sumOf { it.price }
    }

    // Lucro do mês
    suspend fun getMonthProfit(): Double {
        val monthBookings = getMonthBookings()
        return monthBookings.sumOf { it.price }
    }

    private fun getTodayDate(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // Mude para o formato correto
        return formatter.format(Date())
    }

    private fun getFirstDateOfMonth(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // Mude para o formato correto
        return formatter.format(calendar.time)
    }


    fun confirmBooking(bookingId: String, callback: (Boolean) -> Unit) {
        val bookingRef = firestore.collection("bookings").document(bookingId)

        bookingRef.update("confirmed", true) // Atualiza o status para confirmado
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

}
