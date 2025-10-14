package com.conect.aplicativoconect.data.repositories

import android.util.Log
import com.conect.aplicativoconect.data.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * REPOSITÓRIO ANTI-FRAUD PARA ESTABELECIMENTOS
 * Sistema robusto de validação e prevenção de fraudes em cadastros empresariais
 */
class BusinessValidationRepository {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        // LIMITES DE SEGURANÇA
        const val MAX_BUSINESSES_PER_CPF = 3
        const val MAX_BUSINESSES_PER_DEVICE = 2
        const val MAX_BUSINESSES_PER_ADDRESS = 5
        const val VALIDATION_EXPIRY_DAYS = 7
        const val FRAUD_SCORE_THRESHOLD = 70
        
        // PADRÕES DE VALIDAÇÃO
        val CPF_PATTERN = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")
        val PHONE_PATTERN = Pattern.compile("\\(\\d{2}\\)\\s\\d{4,5}-\\d{4}")
    }
    
    /**
     * VALIDAÇÃO COMPLETA ANTI-FRAUD DO NEGÓCIO
     */
    suspend fun validateBusinessRegistration(
        cpf: String,
        phone: String,
        address: String,
        deviceInfo: String,
        androidId: String,
        ipAddress: String,
        latitude: Double,
        longitude: Double
    ): BusinessValidationResult = withContext(Dispatchers.IO) {
        
        try {
            val validation = BusinessValidation(
                businessId = "",
                ownerId = auth.currentUser?.uid ?: "",
                cpf = cpf,
                phone = phone,
                deviceFingerprint = generateDeviceFingerprint(deviceInfo),
                androidId = androidId,
                ipAddress = ipAddress,
                registrationLocation = BusinessLocation(
                    latitude = latitude,
                    longitude = longitude,
                    address = address
                )
            )
            
            var fraudScore = 0
            val fraudFlags = mutableListOf<String>()
            
            // VALIDAÇÃO 1: Verificar CPF duplicado
            val cpfCount = checkCPFUsage(cpf)
            if (cpfCount >= MAX_BUSINESSES_PER_CPF) {
                fraudFlags.add(BusinessFraudFlag.DUPLICATE_CPF.name)
                fraudScore += 40
            }
            
            // VALIDAÇÃO 2: Verificar telefone duplicado
            val phoneCount = checkPhoneUsage(phone)
            if (phoneCount >= 2) { // Máximo 2 empresas por telefone
                fraudFlags.add(BusinessFraudFlag.DUPLICATE_PHONE.name)
                fraudScore += 30
            }
            
            // VALIDAÇÃO 3: Verificar endereço duplicado
            val addressCount = checkAddressUsage(address)
            if (addressCount >= MAX_BUSINESSES_PER_ADDRESS) {
                fraudFlags.add(BusinessFraudFlag.DUPLICATE_ADDRESS.name)
                fraudScore += 25
            }
            
            // VALIDAÇÃO 4: Verificar dispositivo suspeito
            val deviceCount = checkDeviceUsage(androidId)
            if (deviceCount >= MAX_BUSINESSES_PER_DEVICE) {
                fraudFlags.add(BusinessFraudFlag.SUSPICIOUS_DEVICE.name)
                fraudScore += 35
            }
            
            // VALIDAÇÃO 5: Verificar padrão temporal
            val temporalRisk = checkTemporalPattern(androidId, ipAddress)
            if (temporalRisk > 0.7) {
                fraudFlags.add(BusinessFraudFlag.SUSPICIOUS_TIMING.name)
                fraudScore += 20
            }
            
            // VALIDAÇÃO 6: Validar CPF formato
            if (!isValidCPFFormat(cpf)) {
                fraudScore += 15
            }
            
            // VALIDAÇÃO 7: Verificar IP de alto risco
            val ipRisk = checkIPRisk(ipAddress)
            if (ipRisk > 0.8) {
                fraudFlags.add(BusinessFraudFlag.HIGH_RISK_IP.name)
                fraudScore += 25
            }
            
            // DECISÃO FINAL
            val validationStatus = when {
                fraudScore >= FRAUD_SCORE_THRESHOLD -> BusinessValidationStatus.FRAUD_DETECTED
                fraudScore >= 50 -> BusinessValidationStatus.UNDER_REVIEW
                fraudScore >= 30 -> BusinessValidationStatus.ADDITIONAL_INFO
                else -> BusinessValidationStatus.APPROVED
            }
            
            val validationResult = validation.copy(
                fraudScore = fraudScore,
                fraudFlags = fraudFlags,
                validationStatus = validationStatus,
                lastValidationAt = System.currentTimeMillis()
            )
            
            // NÃO SALVAR AQUI - Será salvo após autenticação bem-sucedida
            
            Log.d("BusinessValidation", "Validação concluída - Score: $fraudScore, Status: $validationStatus")
            
            BusinessValidationResult(
                isValid = validationStatus == BusinessValidationStatus.APPROVED,
                validationStatus = validationStatus,
                fraudScore = fraudScore,
                fraudFlags = fraudFlags,
                requiresReview = validationStatus == BusinessValidationStatus.UNDER_REVIEW,
                requiresDocuments = validationStatus == BusinessValidationStatus.ADDITIONAL_INFO,
                validationData = validationResult // Retornar os dados para salvar depois
            )
            
        } catch (e: Exception) {
            Log.e("BusinessValidation", "Erro na validação: ${e.message}")
            BusinessValidationResult(
                isValid = false,
                validationStatus = BusinessValidationStatus.REJECTED,
                errorMessage = "Erro interno de validação"
            )
        }
    }
    
    /**
     * VERIFICAÇÕES ESPECÍFICAS DE DUPLICAÇÃO
     */
    private suspend fun checkCPFUsage(cpf: String): Int {
        return db.collection("business")
            .whereEqualTo("cpf", cpf)
            .whereEqualTo("isActive", true)
            .get().await().size()
    }
    
    private suspend fun checkPhoneUsage(phone: String): Int {
        return db.collection("business")
            .whereEqualTo("phone", phone)
            .whereEqualTo("isActive", true)
            .get().await().size()
    }
    
    private suspend fun checkAddressUsage(address: String): Int {
        return db.collection("business")
            .whereEqualTo("address", address)
            .whereEqualTo("isActive", true)
            .get().await().size()
    }
    
    private suspend fun checkDeviceUsage(androidId: String): Int {
        return db.collection("business")
            .whereEqualTo("androidId", androidId)
            .whereEqualTo("isActive", true)
            .get().await().size()
    }
    
    /**
     * ANÁLISE TEMPORAL DE PADRÕES SUSPEITOS
     */
    private suspend fun checkTemporalPattern(androidId: String, ipAddress: String): Double {
        val last24h = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        // Verificar registros recentes do mesmo dispositivo/IP
        val recentRegistrations = db.collection("businessValidation")
            .whereEqualTo("androidId", androidId)
            .whereGreaterThan("createdAt", last24h)
            .get().await().size()

        val ipRegistrations = db.collection("businessValidation")
            .whereEqualTo("ipAddress", ipAddress)
            .whereGreaterThan("createdAt", last24h)
            .get().await().size()

        return when {
            recentRegistrations > 3 || ipRegistrations > 5 -> 0.9 // Alto risco
            recentRegistrations > 1 || ipRegistrations > 2 -> 0.6 // Médio risco
            else -> 0.2 // Baixo risco
        }
    }    /**
     * VALIDAÇÕES DE FORMATO E QUALIDADE
     */
    private fun isValidCPFFormat(cpf: String): Boolean {
        return CPF_PATTERN.matcher(cpf).matches() && isValidCPFDigits(cpf)
    }
    
    private fun isValidCPFDigits(cpf: String): Boolean {
        val digits = cpf.replace("[^0-9]".toRegex(), "")
        if (digits.length != 11 || digits.all { it == digits[0] }) return false
        
        // Validar dígitos verificadores
        val firstDigit = calculateCPFDigit(digits.substring(0, 9), intArrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2))
        val secondDigit = calculateCPFDigit(digits.substring(0, 10), intArrayOf(11, 10, 9, 8, 7, 6, 5, 4, 3, 2))
        
        return digits[9].digitToInt() == firstDigit && digits[10].digitToInt() == secondDigit
    }
    
    private fun calculateCPFDigit(digits: String, weights: IntArray): Int {
        val sum = digits.mapIndexed { index, char -> char.digitToInt() * weights[index] }.sum()
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }
    
    /**
     * ANÁLISE DE RISCO DE IP
     */
    private suspend fun checkIPRisk(ipAddress: String): Double {
        // TODO: Integrar com serviços de geolocalização e blacklists
        // Por enquanto, verificação básica
        return when {
            ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.") -> 0.3 // IP local
            ipAddress.startsWith("127.") -> 0.8 // Localhost suspeito
            else -> 0.4 // IP público normal
        }
    }
    
    /**
     * GERAR FINGERPRINT DO DISPOSITIVO
     */
    private fun generateDeviceFingerprint(deviceInfo: String): String {
        return "${deviceInfo.hashCode()}_${System.currentTimeMillis()}".take(32)
    }
    
    /**
     * SALVAR RESULTADO DA VALIDAÇÃO
     */
    suspend fun saveValidationResult(validation: BusinessValidation) {
        val validationId = "${validation.ownerId}_${System.currentTimeMillis()}"
        
        db.collection("businessValidation")
            .document(validationId)
            .set(validation)
            .await()
    }
    
    /**
     * LIMPEZA DE VALIDAÇÕES EXPIRADAS
     */
    suspend fun cleanExpiredValidations() = withContext(Dispatchers.IO) {
        val expiryTime = System.currentTimeMillis() - (VALIDATION_EXPIRY_DAYS * 24 * 60 * 60 * 1000)
        
        val expiredValidations = db.collection("businessValidation")
            .whereLessThan("createdAt", expiryTime)
            .whereEqualTo("validationStatus", BusinessValidationStatus.PENDING.name)
            .get().await()
        
        expiredValidations.documents.forEach { doc ->
            doc.reference.delete()
        }
        
        Log.d("BusinessValidation", "Limpeza: ${expiredValidations.size()} validações expiradas removidas")
    }
}

/**
 * RESULTADO DA VALIDAÇÃO ANTI-FRAUD
 */
data class BusinessValidationResult(
    val isValid: Boolean,
    val validationStatus: BusinessValidationStatus,
    val fraudScore: Int = 0,
    val fraudFlags: List<String> = emptyList(),
    val requiresReview: Boolean = false,
    val requiresDocuments: Boolean = false,
    val errorMessage: String? = null,
    val nextSteps: List<String> = emptyList(),
    val validationData: BusinessValidation? = null // Dados para salvar após autenticação
)