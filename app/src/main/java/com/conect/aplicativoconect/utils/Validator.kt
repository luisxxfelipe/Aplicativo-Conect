package com.conect.aplicativoconect.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

object Validator {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
    private val phoneRegex = Regex("^\\(\\\\d{2}\\) \\\\d{4,5}-\\\\d{4}$")  // (11) 99999-9999 ou (11) 9999-9999

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email)

    fun isValidPhone(phone: String): Boolean = phoneRegex.matches(phone.replace(" ", "").replace("-", "")) && phone.length >= 14  // Após máscara

    fun isValidCPF(cpf: String): Boolean {
        val cleanCpf = cpf.replace(Regex("[^\\d]"), "")
        if (cleanCpf.length != 11 || cleanCpf.all { it == cleanCpf[0] }) return false

        // Primeiro dígito
        var sum = 0
        for (i in 0 until 9) sum += cleanCpf[i].digitToInt() * (10 - i)
        var firstCheck = 11 - (sum % 11)
        if (firstCheck >= 10) firstCheck = 0
        if (cleanCpf[9].digitToInt() != firstCheck) return false

        // Segundo dígito
        sum = 0
        for (i in 0 until 10) sum += cleanCpf[i].digitToInt() * (11 - i)
        var secondCheck = 11 - (sum % 11)
        if (secondCheck >= 10) secondCheck = 0
        if (cleanCpf[10].digitToInt() != secondCheck) return false

        return true
    }

    fun isValidDateTime(date: String, time: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateTime = formatter.parse("$date $time")
            dateTime != null && dateTime.after(Date())  // Deve ser futuro
        } catch (e: Exception) {
            false
        }
    }
}
