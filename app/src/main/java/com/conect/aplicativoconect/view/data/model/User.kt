package com.conect.aplicativoconect.view.data.model

data class User(
    val email: String = "",        // Valor padrão para email
    val displayName: String = "",  // Valor padrão para displayName
    val userType: String = ""      // Valor padrão para userType
) {
    // Construtor sem argumentos
    constructor() : this("", "", "")
}