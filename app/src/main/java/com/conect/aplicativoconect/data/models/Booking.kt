package com.conect.aplicativoconect.data.models

data class Booking(
    val name: String? = null,
    val phoneCliente: String = "",
    var id: String? = null,
    val date: String? = null,
    val hour: String = "",
    var rating: Map<String, Any>? = null,
    var companyName: String? = null,
    val serviceName: String? = null,
    var fcmToken: String? = null,
    val userId: String? = null,
    var timestamp: Long? = null,
    val companyId: String? = null,
    var price: Double = 0.0,
    val userImageUrl: String? = null,
    var status_cliente: String? = null,
    var status_adm: String? = null,
    var businessPhone: String? = null
)
