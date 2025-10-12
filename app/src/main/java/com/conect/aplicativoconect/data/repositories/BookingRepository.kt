package com.conect.aplicativoconect.data.repositories

import android.util.Log
import com.conect.aplicativoconect.data.models.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.conect.aplicativoconect.utils.Validator
import java.util.Calendar
import java.util.Date

class BookingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    // ✅ CACHE DE PREÇOS DE SERVIÇOS - Evita queries repetidas
    private val servicePriceCache = mutableMapOf<String, Map<String, Double>>()
    private val businessServicesCache = mutableMapOf<String, List<Map<String, Any>>>()

    // Obtém agendamentos da semana para uma empresa específica
    suspend fun getWeeklyBookingsByCompany(businessId: String): List<Booking> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startOfWeek = calendar.time

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        val endOfWeek = calendar.time

        val formatter = Validator.DATE_FORMAT // ✅ OTIMIZADO: Formatador central

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
        val formatter = Validator.DATE_FORMAT // ✅ OTIMIZADO: Formatador central

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

    // ✅ OTIMIZADO: Calcula lucro total com cache de preços
    private suspend fun calculateTotalProfit(businessId: String, bookings: List<Booking>): Double {
        // Buscar todos os preços de uma vez (otimização)
        val businessPrices = getBusinessServicePrices(businessId)
        
        var totalProfit = 0.0
        for (booking in bookings) {
            val serviceName = booking.serviceName ?: continue
            val servicePrice = businessPrices[serviceName] ?: 0.0
            totalProfit += servicePrice
        }

        Log.d("BookingRepository", "Lucro total calculado: $totalProfit")
        return totalProfit
    }

    // ✅ OTIMIZADO: Cache de preços para evitar queries repetidas
    suspend fun getServicePrice(businessId: String, serviceName: String): Double {
        // Verificar cache primeiro
        servicePriceCache[businessId]?.get(serviceName)?.let { return it }
        
        return try {
            val businessSnapshot = firestore.collection("business")
                .document(businessId)
                .get()
                .await()

            val services = businessSnapshot["services"] as? List<Map<String, Any>> ?: emptyList()
            
            // Cache todos os serviços da empresa
            val priceMap = services.associate { service ->
                val name = service["serviceName"] as? String ?: ""
                val price = (service["price"] as? Number)?.toDouble() ?: 0.0
                name to price
            }
            servicePriceCache[businessId] = priceMap
            
            val price = priceMap[serviceName] ?: 0.0
            Log.d("BookingRepository", "Preço do serviço '$serviceName': $price (cached)")
            price
        } catch (e: Exception) {
            Log.e("BookingRepository", "Erro ao buscar preço do serviço: ", e)
            0.0
        }
    }

    // ✅ NOVA: Busca todos os preços de uma empresa de uma vez
    private suspend fun getBusinessServicePrices(businessId: String): Map<String, Double> {
        // Retornar cache se disponível
        servicePriceCache[businessId]?.let { return it }
        
        return try {
            val businessSnapshot = firestore.collection("business")
                .document(businessId)
                .get()
                .await()

            val services = businessSnapshot["services"] as? List<Map<String, Any>> ?: emptyList()
            val priceMap = services.associate { service ->
                val name = service["serviceName"] as? String ?: ""
                val price = (service["price"] as? Number)?.toDouble() ?: 0.0
                name to price
            }
            
            // Armazenar em cache
            servicePriceCache[businessId] = priceMap
            Log.d("BookingRepository", "Preços carregados e cached para business $businessId")
            priceMap
        } catch (e: Exception) {
            Log.e("BookingRepository", "Erro ao buscar preços dos serviços: ", e)
            emptyMap()
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
            // ✅ FCM configurado via UpcomingBookingWorker
        }

    private fun getFirstDateOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.time
    }

    private fun parseDateTime(date: String, hour: String): Date? {
        return try {
            Validator.DATE_TIME_FORMAT.parse("$date $hour") // ✅ OTIMIZADO: Formatador central
        } catch (e: Exception) {
            Log.e("BookingRepository", "Erro ao parsear data/hora: ", e)
            null
        }
    }
}
