package com.conect.aplicativoconect.view.data.model

data class User(
    val email: String = "",        // Valor padrão para email
    val name: String = "",         // Valor padrão para name
    val cpf: String = "",          // Valor padrão para cpf
    val userType: String = "",     // Valor padrão para userType
    val imageUrl: String
) {
    // Construtor sem argumentos
    constructor() : this("", "", "", "", "")
}
