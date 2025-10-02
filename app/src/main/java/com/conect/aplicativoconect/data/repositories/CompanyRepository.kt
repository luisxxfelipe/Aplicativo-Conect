package com.conect.aplicativoconect.data.repositories

import com.conect.aplicativoconect.data.models.Business
import com.conect.aplicativoconect.data.models.Service
import com.conect.aplicativoconect.data.models.ServicePhoto
import com.conect.aplicativoconect.ui.viewmodels.CompanyViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CompanyRepository {
    
    private val db = FirebaseFirestore.getInstance()
    
    suspend fun getCompanyById(companyId: String): Business? = withContext(Dispatchers.IO) {
        try {
            val document = db.collection("business").document(companyId).get().await()
            if (document.exists()) {
                document.toObject(Business::class.java)?.apply {
                    id = document.id
                }
            } else null
        } catch (e: Exception) {
            throw Exception("Erro ao buscar empresa: ${e.message}")
        }
    }
    
    suspend fun getCompanyServices(companyId: String): List<Service> = withContext(Dispatchers.IO) {
        try {
            val document = db.collection("business").document(companyId).get().await()
            if (document.exists()) {
                val serviceList = document.get("serviceList") as? List<Map<String, Any>>
                serviceList?.mapNotNull { serviceMap ->
                    try {
                        Service(
                            name = serviceMap["name"] as? String ?: "",
                            price = (serviceMap["price"] as? Number)?.toDouble() ?: 0.0,
                            duration = (serviceMap["duration"] as? Number)?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            throw Exception("Erro ao buscar serviços: ${e.message}")
        }
    }
    
    suspend fun getCompanyPhotos(companyId: String): List<ServicePhoto> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("photos")
                .whereEqualTo("companyId", companyId)
                .get()
                .await()
                
            querySnapshot.documents.mapNotNull { document ->
                document.toObject(ServicePhoto::class.java)?.apply {
                    id = document.id
                }
            }
        } catch (e: Exception) {
            throw Exception("Erro ao buscar fotos: ${e.message}")
        }
    }
    
    suspend fun getCompanyReviews(companyId: String): List<CompanyViewModel.ReviewItem> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = db.collection("ratings")
                .whereEqualTo("companyId", companyId)
                .get()
                .await()
                
            querySnapshot.documents.mapNotNull { document ->
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
        } catch (e: Exception) {
            throw Exception("Erro ao buscar avaliações: ${e.message}")
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