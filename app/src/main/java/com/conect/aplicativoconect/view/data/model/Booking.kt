package com.conect.aplicativoconect.view.data.model

data class Booking(
    val id: String = "",
    val date: String = "",
    val time: String = "",
    val service: String = "",
    val userId: String = "",
    val businessId: String = "",
    val price: Double = 0.0
)