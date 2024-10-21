package com.conect.aplicativoconect.view.data.model

data class User(
    val email: String = "",        // Valor padrão para email
    val name: String = "",         // Valor padrão para name
    val userType: String = ""      // Valor padrão para userType
) {
    // Construtor sem argumentos
    constructor() : this("", "", "")
}
