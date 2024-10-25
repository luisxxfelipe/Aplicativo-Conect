package com.conect.aplicativoconect.view.data.model

// Classe para representar os horários de funcionamento
data class OperatingHours(
    val opening: String = "",  // Horário de abertura
    val closing: String = "",   // Horário de fechamento
    val days: List<String> = listOf()  // Lista de dias
)

data class Business(
    val name: String,
    val description: String,
    val serviceType: String,
    val address: String,
    val phone: String,
    val operatingHours: OperatingHours,
    val imageUrl: String,
    val email: String, // Campo email
    val isActive: Boolean // Campo isActive
) {
    // Construtor padrão necessário para o Firestore
    constructor() : this("", "", "", "", "", OperatingHours(), "", "", true) // Definindo isActive como true por padrão
}
