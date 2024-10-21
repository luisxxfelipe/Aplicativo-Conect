package com.conect.aplicativoconect.view.data.model

// Enum para representar os dias da semana
enum class DayOfWeek(val displayName: String) {
    MONDAY("Segunda-feira"),
    TUESDAY("Terça-feira"),
    WEDNESDAY("Quarta-feira"),
    THURSDAY("Quinta-feira"),
    FRIDAY("Sexta-feira"),
    SATURDAY("Sábado"),
    SUNDAY("Domingo")
}

// Classe para representar os horários de funcionamento
data class OperatingHours(
    val opening: String = "",  // Horário de abertura
    val closing: String = "",   // Horário de fechamento
    val days: List<String> = listOf()  // Lista de dias
)

// Modelo do negócio, agora incluindo o `uid`
data class Business(
    val uid: String = "",              // ID único do negócio
    val name: String = "",             // Nome do negócio
    val address: String = "",          // Endereço do negócio
    val phone: String = "",            // Telefone do negócio
    val operatingHours: OperatingHours = OperatingHours(), // Horário de funcionamento
    val serviceType: String = "",      // Tipo de serviço oferecido
    var imageUrl: String? = null
) {
    // Construtor padrão necessário para o Firestore
    constructor() : this("", "", "", "", OperatingHours(), "")
}
