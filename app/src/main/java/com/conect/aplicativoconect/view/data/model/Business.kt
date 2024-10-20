package com.conect.aplicativoconect.view.data.model

data class Business(
    val uid: String = "",              // ID único do negócio
    val name: String = "",             // Nome do negócio
    val address: String = "",          // Endereço do negócio
    val phone: String = "",            // Telefone do negócio
    val operatingHours: String = "",   // Horário de funcionamento
    val serviceType: String = ""       // Tipo de serviço oferecido
) {
    // Construtor padrão necessário para o Firestore
    constructor() : this("", "", "", "", "", "")
}
