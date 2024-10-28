package com.conect.aplicativoconect.view.data.model

data class Booking(
    val name: String? = null,
    var id: String? = null,
    val date: String? = null,
    val hour: Int? = null,
    val serviceName: String? = null,
    var fcmToken: String? = null,
    val userId: String? = null,
    val companyId: String? = null,
    var price: Double = 0.0,
    val userImageUrl: String? = null,
    var status_cliente: String? = null,
    var status_adm: String? = null
)
