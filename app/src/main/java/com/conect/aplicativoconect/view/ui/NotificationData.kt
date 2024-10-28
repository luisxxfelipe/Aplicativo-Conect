package com.conect.aplicativoconect.view.ui

data class NotificationData(
    val to: String,
    val notification: NotificationBody
)

data class NotificationBody(
    val title: String,
    val body: String
)
