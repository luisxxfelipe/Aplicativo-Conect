package com.conect.aplicativoconect.view.data.model

import java.io.Serializable

data class Service(
    val name: String = "",  // Adicione valores padrão
    val price: Double = 0.0
) : Serializable {
    // Construtor sem argumentos necessário para o Firestore
    constructor() : this("", 0.0)
}