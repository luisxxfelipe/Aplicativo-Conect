package com.conect.aplicativoconect.data.repositories

import android.util.Log
import com.conect.aplicativoconect.data.models.Business
import com.conect.aplicativoconect.data.models.Service
import com.conect.aplicativoconect.data.models.ServicePhoto
import com.conect.aplicativoconect.ui.viewmodels.CompanyViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CompanyRepository {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    suspend fun getCompanyById(companyId: String): Business? = withContext(Dispatchers.IO) {
        try {
            if (!isUserAuthenticated()) {
                Log.e("CompanyRepository", "Usuário não autenticado")
                throw Exception("Usuário não autenticado")
            }
            
            Log.d("CompanyRepository", "Buscando empresa: $companyId")
            val document = db.collection("business").document(companyId).get().await()
            
            if (document.exists()) {
                Log.d("CompanyRepository", "Empresa encontrada: ${document.id}")
                document.toObject(Business::class.java)?.apply {
                    id = document.id
                }
            } else {
                Log.w("CompanyRepository", "Empresa não encontrada: $companyId")
                null
            }
        } catch (e: Exception) {
            Log.e("CompanyRepository", "Erro ao buscar empresa: ${e.message}", e)
            throw Exception("Erro ao buscar empresa: ${e.message}")
        }
    }
    
    suspend fun getCompanyServices(companyId: String): List<Service> = withContext(Dispatchers.IO) {
        try {
            if (!isUserAuthenticated()) {
                Log.e("CompanyRepository", "Usuário não autenticado para buscar serviços")
                throw Exception("Usuário não autenticado")
            }
            
            Log.d("CompanyRepository", "Buscando serviços da empresa: $companyId")
            val document = db.collection("business").document(companyId).get().await()
            
            if (document.exists()) {
                // CORRIGIDO: Mudança de "serviceList" para "services" conforme estrutura do Firestore
                val serviceList = document.get("services") as? List<Map<String, Any>>
                val services = serviceList?.mapNotNull { serviceMap ->
                    try {
                        Service(
                            // CORRIGIDO: Mudança de "name" para "serviceName" conforme estrutura do Firestore
                            name = serviceMap["serviceName"] as? String ?: "",
                            price = (serviceMap["price"] as? Number)?.toDouble() ?: 0.0,
                            duration = (serviceMap["duration"] as? Number)?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        Log.w("CompanyRepository", "Erro ao mapear serviço: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                Log.d("CompanyRepository", "Encontrados ${services.size} serviços")
                services
            } else {
                Log.w("CompanyRepository", "Documento da empresa não encontrado: $companyId")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("CompanyRepository", "Erro ao buscar serviços: ${e.message}", e)
            throw Exception("Erro ao buscar serviços: ${e.message}")
        }
    }
    
    suspend fun getCompanyPhotos(companyId: String): List<ServicePhoto> = withContext(Dispatchers.IO) {
        try {
            if (!isUserAuthenticated()) {
                Log.e("CompanyRepository", "Usuário não autenticado para buscar fotos")
                throw Exception("Usuário não autenticado")
            }
            
            Log.d("CompanyRepository", "Buscando fotos da empresa: $companyId")
            
            // 🔧 CORRIGIDO: Buscar na subcoleção servicePhotos conforme estrutura do Firebase
            val querySnapshot = db.collection("business")
                .document(companyId)
                .collection("servicePhotos")
                .get()
                .await()
                
            val photos = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data
                    ServicePhoto(
                        id = document.id,
                        url = data?.get("url") as? String ?: "",
                        caption = data?.get("caption") as? String ?: ""
                    )
                } catch (e: Exception) {
                    Log.w("CompanyRepository", "Erro ao mapear foto: ${e.message}")
                    null
                }
            }
            
            Log.d("CompanyRepository", "Encontradas ${photos.size} fotos")
            photos
        } catch (e: Exception) {
            Log.e("CompanyRepository", "Erro ao buscar fotos: ${e.message}", e)
            throw Exception("Erro ao buscar fotos: ${e.message}")
        }
    }
    
    suspend fun getCompanyReviews(companyId: String): List<CompanyViewModel.ReviewItem> = withContext(Dispatchers.IO) {
        try {
            if (!isUserAuthenticated()) {
                Log.e("CompanyRepository", "Usuário não autenticado para buscar avaliações")
                throw Exception("Usuário não autenticado")
            }
            
            Log.d("CompanyRepository", "Buscando avaliações da empresa: $companyId")
            
            // 🔧 CORRIGIDO: Buscar avaliações nos bookings que têm rating e são dessa empresa
            val querySnapshot = db.collection("bookings")
                .whereEqualTo("companyId", companyId)
                .whereNotEqualTo("rating", null)
                .get()
                .await()
                
            val reviews = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data
                    val rating = data?.get("rating") as? Map<*, *>
                    val clientName = data?.get("clientName") as? String ?: "Cliente"
                    val serviceName = data?.get("serviceName") as? String ?: ""
                    
                    if (rating != null) {
                        val quality = (rating["quality"] as? Number)?.toLong() ?: 0L
                        val punctuality = (rating["punctuality"] as? Number)?.toLong() ?: 0L
                        val service = (rating["service"] as? Number)?.toLong() ?: 0L
                        val comment = rating["comment"] as? String ?: ""
                        
                        CompanyViewModel.ReviewItem(
                            name = clientName,
                            comment = if (comment.isNotBlank()) comment else "Serviço: $serviceName",
                            quality = quality,
                            punctuality = punctuality,
                            service = service
                        )
                    } else null
                } catch (e: Exception) {
                    Log.w("CompanyRepository", "Erro ao mapear avaliação: ${e.message}")
                    null
                }
            }
            
            Log.d("CompanyRepository", "Encontradas ${reviews.size} avaliações")
            reviews
        } catch (e: Exception) {
            Log.e("CompanyRepository", "Erro ao buscar avaliações: ${e.message}", e)
            // 🔄 Fallback: Tentar buscar em coleção separada "ratings" caso exista
            try {
                Log.d("CompanyRepository", "Tentando buscar em coleção 'ratings' como fallback")
                val ratingsSnapshot = db.collection("ratings")
                    .whereEqualTo("companyId", companyId)
                    .get()
                    .await()
                    
                val fallbackReviews = ratingsSnapshot.documents.mapNotNull { document ->
                    try {
                        val data = document.data
                        val name = data?.get("userName") as? String ?: "Usuário"
                        val comment = data?.get("comment") as? String ?: ""
                        val quality = (data?.get("quality") as? Number)?.toLong() ?: 0L
                        val punctuality = (data?.get("punctuality") as? Number)?.toLong() ?: 0L
                        val service = (data?.get("service") as? Number)?.toLong() ?: 0L
                        
                        CompanyViewModel.ReviewItem(name, comment, quality, punctuality, service)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                Log.d("CompanyRepository", "Encontradas ${fallbackReviews.size} avaliações no fallback")
                fallbackReviews
            } catch (fallbackException: Exception) {
                Log.e("CompanyRepository", "Erro no fallback de avaliações: ${fallbackException.message}")
                emptyList()
            }
        }
    }
    
    suspend fun getOperatingHours(companyId: String): OperatingHours? = withContext(Dispatchers.IO) {
        try {
            val document = db.collection("business")
                .document(companyId)
                .get()
                .await()
            
            if (document.exists()) {
                val operatingHours = document.get("operatingHours") as? Map<*, *>
                operatingHours?.let {
                    val opening = it["opening"] as? String ?: "7:00"
                    val closing = it["closing"] as? String ?: "19:00"
                    val days = (it["days"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    OperatingHours(opening, closing, days)
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun saveOperatingHours(companyId: String, operatingHours: OperatingHours): Boolean = withContext(Dispatchers.IO) {
        try {
            val hours = hashMapOf(
                "opening" to operatingHours.opening,
                "closing" to operatingHours.closing,
                "days" to operatingHours.days
            )
            
            db.collection("business")
                .document(companyId)
                .set(mapOf("operatingHours" to hours), com.google.firebase.firestore.SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    data class OperatingHours(
        val opening: String,
        val closing: String,
        val days: List<String>
    )
}