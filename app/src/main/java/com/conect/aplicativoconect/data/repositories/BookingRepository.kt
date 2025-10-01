package com.conect.aplicativoconect.data.repositories

import android.util.Log
import com.conect.aplicativoconect.data.models.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BookingRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // Obtém agendamentos da semana para uma empresa específica
    suspend fun getWeeklyBookingsByCompany(businessId: String): List<Booking> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startOfWeek = calendar.time

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        val endOfWeek = calendar.time

        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val snapshot = firestore.collection("bookings")
            .whereEqualTo("companyId", businessId)
            .whereGreaterThanOrEqualTo("date", formatter.format(startOfWeek))
            .whereLessThanOrEqualTo("date", formatter.format(endOfWeek))
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(Booking::class.java)?.apply {
                id = document.id
            }
        }.filter { booking ->
            booking.status_adm != "canceled" && booking.status_cliente != "canceled"
        }
    }

    // Obtém agendamentos do mês para uma empresa específica
    suspend fun getMonthBookingsByCompany(businessId: String): List<Booking> {
        val startOfMonth = getFirstDateOfMonth()
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val snapshot = firestore.collection("bookings")
            .whereEqualTo("companyId", businessId)
            .whereGreaterThanOrEqualTo("date", formatter.format(startOfMonth))
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(Booking::class.java)?.apply {
                id = document.id
            }
        }.filter { booking ->
            booking.status_adm != "canceled" && booking.status_cliente != "canceled"
        }
    }

    // Calcula o lucro semanal para uma empresa específica
    suspend fun getWeeklyProfitByCompany(businessId: String): Double {
        val bookings = getWeeklyBookingsByCompany(businessId)
        return calculateTotalProfit(businessId, bookings)
    }

    // Calcula o lucro mensal para uma empresa específica
    suspend fun getMonthProfitByCompany(businessId: String): Double {
        val bookings = getMonthBookingsByCompany(businessId)
        return calculateTotalProfit(businessId, bookings)
    }

    // Calcula o lucro total baseado nos agendamentos e preços dos serviços
    private suspend fun calculateTotalProfit(businessId: String, bookings: List<Booking>): Double {
        var totalProfit = 0.0

        for (booking in bookings) {
            val serviceName = booking.serviceName ?: continue
            val servicePrice = getServicePrice(businessId, serviceName)
            totalProfit += servicePrice
        }

        Log.d("BookingRepository", "Lucro total: $totalProfit")
        return totalProfit
    }

    // Obtém o preço de um serviço específico de uma empresa
    suspend fun getServicePrice(businessId: String, serviceName: String): Double {
        return try {
            val businessSnapshot = firestore.collection("business")
                .document(businessId)
                .get()
                .await()

            val services = businessSnapshot["services"] as? List<Map<String, Any>> ?: emptyList()
            val service = services.find { it["serviceName"] == serviceName }
            val price = (service?.get("price") as? Number)?.toDouble() ?: 0.0

            Log.d("BookingRepository", "Preço do serviço '$serviceName': $price")
            price
        } catch (e: Exception) {
            Log.e("BookingRepository", "Erro ao buscar preço do serviço: ", e)
            0.0
        }
    }

    suspend fun getUserBookings(userId: String) = withContext(Dispatchers.IO) {
        val snapshot = firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        snapshot.documents.mapNotNull { document ->
            val booking = document.toObject(Booking::class.java)
            booking?.id = document.id
            booking
        }.sortedWith(compareByDescending<Booking> { parseDateTime(it.date ?: "", it.hour) })
    }

    suspend fun confirmBooking(id: String) = withContext(Dispatchers.IO) {
        firestore.collection("bookings").document(id)
            .update("status_cliente", "confirmed")
            .await()
    }

    suspend fun cancelBooking(id: String) = withContext(Dispatchers.IO) {
        firestore.collection("bookings").document(id).delete().await()
    }

    suspend fun sendNotification(bookingId: String, title: String, message: String) =
        withContext(Dispatchers.IO) {
            // Buscar companyId
            val bookingDoc = firestore.collection("bookings").document(bookingId).get().await()
            val companyId = bookingDoc.getString("companyId") ?: return@withContext

            // Buscar token
            val businessDoc = firestore.collection("business").document(companyId).get().await()
            val token = businessDoc.getString("fcmToken") ?: return@withContext

            // Enviar FCM (use Firebase SDK ou Volley como antes – simplificado)
            // Para agora, log; implemente full FCM se necessário
            Log.d("BookingRepo", "Enviando notificação para $token: $title - $message")
            // TODO: Implementar FCM send com Admin SDK ou HTTP
        }

    private fun getFirstDateOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.time
    }

    private fun parseDateTime(date: String, hour: String): Date? {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return try {
            formatter.parse("$date $hour")
        } catch (e: Exception) {
            Log.e("BookingRepository", "Erro ao parsear data/hora: ", e)
            null
        }
    }
}
