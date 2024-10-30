package com.conect.aplicativoconect.view.data.model

import java.io.Serializable

data class Service(
    val name: String = "",  // Valor padrão para evitar problemas
    val price: Double = 0.0  // Valor padrão para evitar problemas
) : Serializable {
    // Construtor sem argumentos necessário para o Firestore e serialização
    constructor() : this("", 0.0)
}
