package com.conect.aplicativoconect.data.repositories

import android.util.Log
import com.conect.aplicativoconect.data.models.Coupon
import com.conect.aplicativoconect.data.models.Referral
import com.conect.aplicativoconect.data.models.ReferralStatus
import com.conect.aplicativoconect.data.models.UserPoints
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class ReferralRepository {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        const val POINTS_PER_REFERRAL = 100
        const val MAX_REFERRALS_PER_DAY = 5  // 🔒 Limite diário de indicações
        const val POPUP_COOLDOWN_HOURS = 24  // 🔒 Cooldown do popup (24 horas)
        const val REFERRAL_EXPIRY_DAYS = 30  // 🔒 Expiração de indicações pendentes
        
        // Cupons disponíveis
        val AVAILABLE_COUPONS = listOf(
            Triple(500, 10, "CONECT10"),   // 500 pontos = 10% desconto
            Triple(1000, 20, "CONECT20"),  // 1000 pontos = 20% desconto
            Triple(2000, 50, "CONECT50")   // 2000 pontos = 50% desconto
        )
    }
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    suspend fun getUserPoints(userId: String = getCurrentUserId() ?: ""): UserPoints? = withContext(Dispatchers.IO) {
        try {
            if (userId.isEmpty()) return@withContext null
            
            val document = db.collection("userPoints").document(userId).get().await()
            
            if (document.exists()) {
                document.toObject(UserPoints::class.java)
            } else {
                // Criar documento inicial se não existir
                val initialPoints = UserPoints(userId = userId)
                db.collection("userPoints").document(userId).set(initialPoints).await()
                initialPoints
            }
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro ao buscar pontos do usuário: ${e.message}")
            null
        }
    }
    
    suspend fun addReferral(referredUserName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext false
            
            val referralId = UUID.randomUUID().toString()
            val referral = Referral(
                id = referralId,
                referredUserName = referredUserName,
                pointsEarned = POINTS_PER_REFERRAL,
                status = ReferralStatus.PENDING
            )
            
            // Adicionar referral pendente
            db.collection("userPoints").document(userId)
                .collection("referrals").document(referralId)
                .set(referral).await()
            
            Log.d("ReferralRepository", "Referral adicionado: $referredUserName")
            true
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro ao adicionar referral: ${e.message}")
            false
        }
    }
    
    suspend fun completeReferral(userId: String, referredUserName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Buscar referral pendente
            val referralsSnapshot = db.collection("userPoints").document(userId)
                .collection("referrals")
                .whereEqualTo("referredUserName", referredUserName)
                .whereEqualTo("status", ReferralStatus.PENDING.name)
                .get().await()
            
            if (!referralsSnapshot.isEmpty) {
                val referralDoc = referralsSnapshot.documents.first()
                val referralId = referralDoc.id
                
                // Atualizar referral como completo
                db.collection("userPoints").document(userId)
                    .collection("referrals").document(referralId)
                    .update(
                        "status", ReferralStatus.COMPLETED.name,
                        "completedAt", System.currentTimeMillis()
                    ).await()
                
                // Adicionar pontos ao usuário
                addPointsToUser(userId, POINTS_PER_REFERRAL)
                
                Log.d("ReferralRepository", "Referral completado para: $referredUserName")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro ao completar referral: ${e.message}")
            false
        }
    }
    
    private suspend fun addPointsToUser(userId: String, points: Int) {
        try {
            db.runTransaction { transaction ->
                val userPointsRef = db.collection("userPoints").document(userId)
                val snapshot = transaction.get(userPointsRef)
                
                val currentPoints = snapshot.getLong("totalPoints")?.toInt() ?: 0
                val currentReferrals = snapshot.getLong("referralsCount")?.toInt() ?: 0
                
                transaction.update(userPointsRef, mapOf(
                    "totalPoints" to (currentPoints + points),
                    "referralsCount" to (currentReferrals + 1),
                    "lastUpdated" to System.currentTimeMillis()
                ))
                
                null
            }.await()
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro ao adicionar pontos: ${e.message}")
        }
    }
    
    suspend fun redeemCoupon(pointsCost: Int, discountPercent: Int): Coupon? = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext null
            val userPoints = getUserPoints(userId) ?: return@withContext null
            
            if (userPoints.totalPoints < pointsCost) {
                return@withContext null // Pontos insuficientes
            }
            
            val couponCode = generateCouponCode(discountPercent)
            val coupon = Coupon(
                id = UUID.randomUUID().toString(),
                code = couponCode,
                discountPercent = discountPercent,
                pointsCost = pointsCost,
                expiresAt = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000) // 30 dias
            )
            
            // Debitar pontos e adicionar cupom
            db.runTransaction { transaction ->
                val userPointsRef = db.collection("userPoints").document(userId)
                val snapshot = transaction.get(userPointsRef)
                
                val currentPoints = snapshot.getLong("totalPoints")?.toInt() ?: 0
                if (currentPoints >= pointsCost) {
                    transaction.update(userPointsRef, "totalPoints", currentPoints - pointsCost)
                    
                    // Adicionar cupom à coleção
                    val couponRef = db.collection("userPoints").document(userId)
                        .collection("coupons").document(coupon.id)
                    transaction.set(couponRef, coupon)
                }
                null
            }.await()
            
            Log.d("ReferralRepository", "Cupom resgatado: ${coupon.code}")
            coupon
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro ao resgatar cupom: ${e.message}")
            null
        }
    }
    
    private fun generateCouponCode(discountPercent: Int): String {
        val randomSuffix = (1000..9999).random()
        return "CONECT${discountPercent}_$randomSuffix"
    }
    
    suspend fun getUserCoupons(userId: String = getCurrentUserId() ?: ""): List<Coupon> = withContext(Dispatchers.IO) {
        try {
            if (userId.isEmpty()) return@withContext emptyList()
            
            val couponsSnapshot = db.collection("userPoints").document(userId)
                .collection("coupons")
                .whereEqualTo("isUsed", false)
                .get().await()
            
            couponsSnapshot.toObjects(Coupon::class.java)
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro ao buscar cupons: ${e.message}")
            emptyList()
        }
    }
    
    fun generateReferralMessage(userName: String): String {
        return buildString {
            append("Olá! Eu uso o *ConecteX* para encontrar serviços próximos e queria te indicar.\n\n")
            append("📱 *O que é o ConecteX?*\n")
            append("• App para encontrar profissionais autônomos\n")
            append("• Agendamento fácil e rápido\n")
            append("• Avaliações reais dos clientes\n")
            append("• Profissionais verificados\n\n")
            append("✨ *Se você é um profissional autônomo*, pode se cadastrar e ganhar mais clientes!\n\n")
            append("🎯 *Categorias disponíveis:*\n")
            append("• Beleza (Cabeleireiro, Manicure, Estética)\n")
            append("• Técnicos (Informática, Eletrônicos)\n")
            append("• Serviços Gerais (Fotografia, Design)\n")
            append("• E muito mais!\n\n")
            append("📲 *Baixe agora:* [Link do app]\n\n")
            append("_Indicado por: $userName")
        }
    }
    
    // 🔒 ===== MÉTODOS DE SEGURANÇA ANTI-FRAUD =====
    
    /**
     * Verifica se o usuário pode fazer indicações hoje (limite diário)
     */
    suspend fun checkDailyReferralLimit(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val today = System.currentTimeMillis()
            val startOfDay = today - (today % (24 * 60 * 60 * 1000))
            
            val referralsToday = db.collection("userPoints").document(userId)
                .collection("referrals")
                .whereGreaterThan("createdAt", startOfDay)
                .get().await()
            
            referralsToday.size() < MAX_REFERRALS_PER_DAY
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro ao verificar limite diário: ${e.message}")
            false
        }
    }
    
    /**
     * Verifica se deve mostrar popup (cooldown de 24h)
     */
    suspend fun shouldShowReferralPopup(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userPoints = getUserPoints(userId) ?: return@withContext true
            val lastPopup = userPoints.lastPopupShown
            val cooldownTime = POPUP_COOLDOWN_HOURS * 60 * 60 * 1000 // 24h em ms
            
            (System.currentTimeMillis() - lastPopup) >= cooldownTime
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro ao verificar popup: ${e.message}")
            true
        }
    }
    
    /**
     * Atualiza timestamp do último popup mostrado
     */
    suspend fun updateLastPopupShown(userId: String) = withContext(Dispatchers.IO) {
        try {
            db.collection("userPoints").document(userId)
                .update("lastPopupShown", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro ao atualizar popup: ${e.message}")
        }
    }
    
    /**
     * Gera token de segurança para validação anti-fraud
     */
    private fun generateSecurityToken(userId: String, referralCode: String, deviceInfo: String): String {
        val timestamp = System.currentTimeMillis()
        val data = "$userId-$referralCode-$deviceInfo-$timestamp"
        return data.hashCode().toString()
    }
    
    /**
     * Gera fingerprint do dispositivo
     */
    private fun generateDeviceFingerprint(deviceInfo: String): String {
        return deviceInfo.hashCode().toString()
    }
    
    /**
     * Valida referral contra fraudes
     */
    suspend fun validateReferral(
        referralId: String, 
        userId: String, 
        deviceInfo: String, 
        ipAddress: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val referralRef = db.collection("referrals").document(referralId)
            val referralDoc = referralRef.get().await()
            
            if (!referralDoc.exists()) return@withContext false
            
            val fraudFlags = mutableListOf<String>()
            var fraudScore = 0
            
            // 🔒 VALIDAÇÃO 1: Verificar múltiplas indicações do mesmo IP
            val sameIpCount = db.collection("referrals")
                .whereEqualTo("ipAddress", ipAddress)
                .whereGreaterThan("timestamp", System.currentTimeMillis() - (24 * 60 * 60 * 1000))
                .get().await().size()
            
            if (sameIpCount > 3) {
                fraudFlags.add("MULTIPLE_IP_SAME_DAY")
                fraudScore += 30
            }
            
            // 🔒 VALIDAÇÃO 2: Verificar dispositivos similares
            val deviceFingerprint = generateDeviceFingerprint(deviceInfo)
            val similarDevices = db.collection("referrals")
                .whereEqualTo("deviceFingerprint", deviceFingerprint)
                .get().await().size()
            
            if (similarDevices > 2) {
                fraudFlags.add("SIMILAR_DEVICE_MULTIPLE_REFERRALS")
                fraudScore += 40
            }
            
            // 🔒 VALIDAÇÃO 3: Verificar padrão temporal suspeito
            val userReferrals = db.collection("referrals")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp")
                .get().await()
            
            if (userReferrals.size() > 1) {
                val timestamps = userReferrals.documents.map { 
                    it.getTimestamp("timestamp")?.toDate()?.time ?: 0L 
                }
                val intervals = timestamps.zipWithNext { a, b -> b - a }
                val avgInterval = intervals.average()
                
                // Se intervalo muito regular (suspeito de bot)
                if (avgInterval < 60000 && intervals.all { it < 120000 }) { // < 2min
                    fraudFlags.add("SUSPICIOUS_TIMING_PATTERN")
                    fraudScore += 50
                }
            }
            
            // 🔒 DECISÃO ANTI-FRAUD
            val isValid = fraudScore < 50 // Threshold de fraude
            val validationStatus = if (isValid) "APPROVED" else "REJECTED"
            
            // Atualizar documento com resultado da validação
            referralRef.update(mapOf(
                "validationStatus" to validationStatus,
                "fraudScore" to fraudScore,
                "fraudFlags" to fraudFlags,
                "validatedAt" to System.currentTimeMillis(),
                "validationSteps" to mapOf(
                    "ipValidation" to (sameIpCount <= 3),
                    "deviceValidation" to (similarDevices <= 2),
                    "timingValidation" to !fraudFlags.contains("SUSPICIOUS_TIMING_PATTERN")
                )
            )).await()
            
            Log.d("ReferralRepository", "Validação anti-fraud - Score: $fraudScore, Válido: $isValid")
            
            isValid
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro na validação anti-fraud: ${e.message}")
            false
        }
    }
    
    /**
     * Limpa referrals expirados (>30 dias)
     */
    suspend fun cleanExpiredReferrals() = withContext(Dispatchers.IO) {
        try {
            val expiryTime = System.currentTimeMillis() - (REFERRAL_EXPIRY_DAYS * 24 * 60 * 60 * 1000)
            
            val expiredReferrals = db.collection("referrals")
                .whereLessThan("timestamp", expiryTime)
                .whereEqualTo("validationStatus", "PENDING")
                .get().await()
            
            expiredReferrals.documents.forEach { doc ->
                doc.reference.delete()
            }
            
            Log.d("ReferralRepository", "Limpeza: ${expiredReferrals.size()} referrals expirados removidos")
        } catch (e: Exception) {
            Log.e("ReferralRepository", "Erro na limpeza: ${e.message}")
        }
    }
}