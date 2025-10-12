package com.conect.aplicativoconect.data.models

import com.google.firebase.Timestamp

/**
 * 🔒 SISTEMA ANTI-FRAUD PARA ESTABELECIMENTOS
 * Modelo robusto de dados para prevenir criação de estabelecimentos fake
 */
data class BusinessValidation(
    val businessId: String = "",
    val ownerId: String = "",
    val email: String = "",
    val cpf: String = "",
    val phone: String = "",
    
    // 🔒 DADOS DE SEGURANÇA
    val deviceFingerprint: String = "",
    val androidId: String = "",
    val ipAddress: String = "",
    val registrationLocation: BusinessLocation = BusinessLocation(),
    val verificationPhotos: List<String> = emptyList(),
    
    // 🔒 STATUS DE VALIDAÇÃO
    val validationStatus: BusinessValidationStatus = BusinessValidationStatus.PENDING,
    val fraudScore: Int = 0,
    val fraudFlags: List<String> = emptyList(),
    
    // 🔒 VERIFICAÇÕES REALIZADAS
    val cpfValidation: CPFValidationResult = CPFValidationResult(),
    val phoneValidation: PhoneValidationResult = PhoneValidationResult(),
    val addressValidation: AddressValidationResult = AddressValidationResult(),
    val documentValidation: DocumentValidationResult = DocumentValidationResult(),
    
    // 🔒 CONTROLES TEMPORAIS
    val createdAt: Long = System.currentTimeMillis(),
    val lastValidationAt: Long = 0,
    val approvedAt: Long = 0,
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 dias
    
    // 🔒 METADADOS DE AUDITORIA
    val validationAttempts: Int = 0,
    val maxValidationAttempts: Int = 3,
    val isActive: Boolean = true,
    val reviewNotes: String = ""
)

data class BusinessLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val isVerified: Boolean = false,
    val verificationMethod: String = ""
)

data class CPFValidationResult(
    val isValid: Boolean = false,
    val isRealPerson: Boolean = false,
    val name: String = "",
    val status: String = "",
    val validatedAt: Long = 0,
    val source: String = "SERPRO" // API oficial
)

data class PhoneValidationResult(
    val isValid: Boolean = false,
    val isVerified: Boolean = false,
    val carrier: String = "",
    val type: String = "", // mobile, landline
    val verificationCode: String = "",
    val verifiedAt: Long = 0
)

data class AddressValidationResult(
    val isValid: Boolean = false,
    val exists: Boolean = false,
    val isCommercial: Boolean = false,
    val validatedBy: String = "", // Google Places, ViaCEP
    val confidence: Double = 0.0
)

data class DocumentValidationResult(
    val businessLicense: DocumentCheck = DocumentCheck(),
    val identityDocument: DocumentCheck = DocumentCheck(),
    val addressProof: DocumentCheck = DocumentCheck(),
    val allDocumentsValid: Boolean = false
)

data class DocumentCheck(
    val isUploaded: Boolean = false,
    val isValid: Boolean = false,
    val documentType: String = "",
    val verificationMethod: String = "", // OCR, manual
    val confidence: Double = 0.0,
    val verifiedAt: Long = 0
)

enum class BusinessValidationStatus {
    PENDING,           // Aguardando validação
    UNDER_REVIEW,      // Em análise
    ADDITIONAL_INFO,   // Precisa de mais informações
    APPROVED,          // Aprovado
    REJECTED,          // Rejeitado
    SUSPENDED,         // Suspenso por suspeita
    FRAUD_DETECTED     // Fraude detectada
}

enum class BusinessFraudFlag {
    DUPLICATE_CPF,              // CPF já usado
    DUPLICATE_PHONE,            // Telefone já usado
    DUPLICATE_ADDRESS,          // Endereço já usado
    SUSPICIOUS_DEVICE,          // Dispositivo suspeito
    MULTIPLE_REGISTRATIONS,     // Múltiplos registros mesmo dispositivo
    INVALID_DOCUMENTS,          // Documentos inválidos
    FAKE_LOCATION,             // Localização fake
    SUSPICIOUS_TIMING,         // Padrão temporal suspeito
    HIGH_RISK_IP,             // IP de alto risco
    AUTOMATED_BEHAVIOR        // Comportamento de bot
}