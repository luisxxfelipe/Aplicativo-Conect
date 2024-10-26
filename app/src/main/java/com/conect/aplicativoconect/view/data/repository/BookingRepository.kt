package com.conect.aplicativoconect.view.data.repository

import android.util.Log
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

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

    private fun getFirstDateOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.time
    }
}
