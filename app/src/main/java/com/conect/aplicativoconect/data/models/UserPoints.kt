package com.conect.aplicativoconect.data.models

data class UserPoints(
    val userId: String = "",
    val totalPoints: Int = 0,
    val referralsCount: Int = 0,
    val usedCoupons: List<String> = emptyList(),
    val availableCoupons: List<Coupon> = emptyList(),
    val referralHistory: List<Referral> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val lastPopupShown: Long = 0L,  // 🔒 Controle de popup diário
    val securityHash: String = ""   // 🔒 Hash de segurança para validação
)

data class Coupon(
    val id: String = "",
    val code: String = "",
    val discountPercent: Int = 0,
    val pointsCost: Int = 0,
    val isUsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L,
    val securityToken: String = "", // 🔒 Token de segurança único
    val usageHistory: List<CouponUsage> = emptyList()
)

data class CouponUsage(
    val bookingId: String = "",
    val usedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val ipAddress: String = ""
)

data class Referral(
    val id: String = "",
    val referredUserId: String = "",
    val referredUserName: String = "",
    val referredPhone: String = "",      // 🔒 Telefone para validação
    val referredEmail: String = "",      // 🔒 Email para validação
    val pointsEarned: Int = 100,
    val status: ReferralStatus = ReferralStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val deviceId: String = "",           // 🔒 ID do dispositivo que fez a indicação
    val ipAddress: String = "",          // 🔒 IP da indicação
    val validationToken: String = "",    // 🔒 Token de validação única
    val fraudScore: Int = 0              // 🔒 Score de possível fraude (0-100)
)

// 🔒 SISTEMA DE VALIDAÇÃO ROBUSTO
data class ReferralValidation(
    val referralId: String = "",
    val referrerUserId: String = "",
    val referredUserId: String = "",
    val validationSteps: List<ValidationStep> = emptyList(),
    val finalStatus: ValidationResult = ValidationResult.PENDING,
    val fraudFlags: List<FraudFlag> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val validatedAt: Long? = null
)

data class ValidationStep(
    val stepType: ValidationType = ValidationType.EMAIL_VERIFICATION,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val metadata: Map<String, String> = emptyMap()
)

enum class ReferralStatus {
    PENDING,           // Indicação criada, aguardando cadastro
    AWAITING_VALIDATION,  // Usuário se cadastrou, aguardando validação
    VALIDATED,         // Validação completa, aguardando liberação de pontos
    COMPLETED,         // Pontos creditados
    REJECTED_FRAUD,    // Rejeitado por suspeita de fraude
    EXPIRED,           // Expirado (30 dias sem completar)
    CANCELLED          // Cancelado manualmente
}

enum class ValidationType {
    EMAIL_VERIFICATION,    // Verificação por email
    PHONE_VERIFICATION,   // Verificação por SMS
    DEVICE_FINGERPRINT,   // Verificação de dispositivo único
    FIRST_BOOKING,        // Primeiro agendamento realizado
    BUSINESS_REGISTRATION // Registro como profissional validado
}

enum class ValidationResult {
    PENDING,
    APPROVED,
    REJECTED,
    REQUIRES_MANUAL_REVIEW
}

enum class FraudFlag {
    SAME_DEVICE,           // Mesmo dispositivo usado
    SAME_IP_ADDRESS,       // Mesmo IP em curto período
    SUSPICIOUS_EMAIL,      // Email suspeito/temporário
    SUSPICIOUS_PHONE,      // Telefone suspeito/virtual
    RAPID_REGISTRATIONS,   // Muitos registros em pouco tempo
    DUPLICATE_IDENTITY,    // Identidade já cadastrada
    VPN_DETECTED,          // Uso de VPN detectado
    FAKE_DEVICE_ID         // Device ID potencialmente falso
}