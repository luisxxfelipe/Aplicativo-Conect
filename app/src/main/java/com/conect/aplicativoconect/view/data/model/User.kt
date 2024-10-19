package com.conect.aplicativoconect.view.data.model

data class User(
    val email: String,
    val displayName: String,
    val userType: String // Cliente ou Admin
)