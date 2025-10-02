package com.conect.aplicativoconect.data.models

data class ServicePhoto(
    var id: String = "",              // ID do documento no Firestore
    val url: String = "",
    val caption: String = ""
) {
    // Construtor secundário para compatibilidade
    constructor(url: String, caption: String = "") : this("", url, caption)
}