package com.conect.aplicativoconect.view.data.repository

import android.util.Log
import com.conect.aplicativoconect.view.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class BookingRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // Obtém agendamentos confirmados da semana
    suspend fun getWeeklyBookings(): List<Booking> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startOfWeek = calendar.time

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        val endOfWeek = calendar.time

        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        Log.d("BookingRepository", "Início da semana: ${formatter.format(startOfWeek)}")
        Log.d("BookingRepository", "Fim da semana: ${formatter.format(endOfWeek)}")

        val snapshot = firestore.collection("bookings")
            .whereGreaterThanOrEqualTo("date", formatter.format(startOfWeek))
            .whereLessThanOrEqualTo("date", formatter.format(endOfWeek))
            .get()
            .await()

        // Filtra os agendamentos que não têm status "canceled"
        return snapshot.documents.mapNotNull { document ->
            document.toObject(Booking::class.java)?.apply {
                id = document.id
            }
        }.filter { booking ->
            booking.status_adm != "canceled" && booking.status_cliente != "canceled"
        }
    }

    // Método público para obter todos os bookings
    suspend fun getAllBookings(): List<Booking> {
        val snapshot = firestore.collection("bookings").get().await()

        // Filtra os agendamentos que não estão cancelados por nenhuma das partes
        return snapshot.documents.mapNotNull { document ->
            document.toObject(Booking::class.java)
        }.filter { booking ->
            booking.status_adm != "canceled" && booking.status_cliente != "canceled"
        }
    }

    // Obtém agendamentos confirmados do mês
    suspend fun getMonthBookings(): List<Booking> {
        val startOfMonth = getFirstDateOfMonth()
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        Log.d("BookingRepository", "Início do mês: ${formatter.format(startOfMonth)}")

        val snapshot = firestore.collection("bookings")
            .whereGreaterThanOrEqualTo("date", formatter.format(startOfMonth))
            .get()
            .await()

        // Filtra os agendamentos não cancelados
        return snapshot.documents.mapNotNull { document ->
            document.toObject(Booking::class.java)?.apply {
                id = document.id
            }
        }.filter { booking ->
            booking.status_adm != "canceled" && booking.status_cliente != "canceled"
        }
    }

    // Obtém o preço de um serviço específico
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

    // Calcula o lucro semanal
    suspend fun getWeeklyProfit(): Double {
        val bookings = getWeeklyBookings()
        var totalProfit = 0.0

        for (booking in bookings) {
            val businessId = booking.companyId ?: continue
            val serviceName = booking.serviceName ?: continue

            val servicePrice = getServicePrice(businessId, serviceName)
            totalProfit += servicePrice

            Log.d("BookingRepository", "Lucro acumulado da semana: $totalProfit")
        }

        Log.d("BookingRepository", "Lucro total da semana: $totalProfit")
        return totalProfit
    }

    // Calcula o lucro mensal
    suspend fun getMonthProfit(): Double {
        val bookings = getMonthBookings()
        var totalProfit = 0.0

        for (booking in bookings) {
            val businessId = booking.companyId ?: continue
            val serviceName = booking.serviceName ?: continue

            val servicePrice = getServicePrice(businessId, serviceName)
            totalProfit += servicePrice

            Log.d("BookingRepository", "Lucro acumulado do mês: $totalProfit")
        }

        Log.d("BookingRepository", "Lucro total do mês: $totalProfit")
        return totalProfit
    }

    private fun getFirstDateOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.time
    }
}
