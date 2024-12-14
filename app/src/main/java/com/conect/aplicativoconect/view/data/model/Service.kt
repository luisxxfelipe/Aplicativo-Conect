package com.conect.aplicativoconect.view.data.model

import java.io.Serializable

data class Service(
    val name: String = "",  // Nome do serviço
    val price: Double = 0.0, // Preço do serviço
    var duration: Int = 0 // Duração em minutos (valor padrão: 0)
) : Serializable {
    // Construtor sem argumentos necessário para o Firestore
    constructor() : this("", 0.0, 0)
}
