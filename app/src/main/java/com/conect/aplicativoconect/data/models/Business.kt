package com.conect.aplicativoconect.data.models

// Classe para representar os horários de funcionamento
data class OperatingHours(
    val opening: String = "",  // Horário de abertura
    val closing: String = "",   // Horário de fechamento
    val days: List<String> = listOf()  // Lista de dias
)

data class Business(
    var id: String = "",               // ID do documento no Firestore
    val name: String = "",
    val cpf: String = "",
    val businessName: String = "",
    val description: String = "",
    val serviceType: String = "",
    val address: String = "",
    val city: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String = "",
    val operatingHours: OperatingHours = OperatingHours(),
    val imageUrl: String = "",
    val ownerId: String = "",
    val email: String = "",
    val isActive: Boolean = true,
    val services: List<Service> = listOf(),
    var averageRating: Double = 0.0,   // Adicionado: média de avaliações
    var ratingCount: Int = 0           // Adicionado: total de avaliações
) {
    // Construtor padrão para o Firestore
    constructor() : this(
        "",     // id
        "",     // name
        "",     // cpf
        "",     // businessName
        "",     // description
        "",     // serviceType
        "",     // address
        "",     // city
        0.0,    // latitude
        0.0,    // longitude
        "",     // phone
        OperatingHours(), // operatingHours
        "",     // imageUrl
        "",     // ownerId
        "",     // email
        true,   // isActive
        listOf(), // services
        0.0,    // averageRating
        0       // ratingCount
    )
}