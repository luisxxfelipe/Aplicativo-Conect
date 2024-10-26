package com.conect.aplicativoconect.view.data.model

data class Booking(
    val name: String? = null,
    var id: String? = null,
    val date: String? = null,
    val hour: Int? = null,
    val serviceName: String? = null,
    val userId: String? = null,
    val companyId: String? = null,
    val price: Double = 0.0,
    val userImageUrl: String? = null,
    var status_cliente: String? = null,
    val status_adm: String? = null
)
