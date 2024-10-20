package com.conect.aplicativoconect.view.data.repository

import com.conect.aplicativoconect.view.data.model.Booking
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
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getFirstDateOfMonth(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }
}
