package com.conect.aplicativoconect.view.data

data class PaymentPreference(
    val items: List<Item>,
    val payer: Payer,
    val back_urls: BackUrls,
    val auto_return: String = "approved"
)

data class Item(
    val title: String,
    val quantity: Int,
    val unit_price: Float
)

data class Payer(
    val email: String
)

data class BackUrls(
    val success: String,
    val failure: String,
    val pending: String
)

data class PreferenceResponse(
    val id: String,
    val init_point: String // URL do checkout
)