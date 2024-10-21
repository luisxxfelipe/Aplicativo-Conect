package com.conect.aplicativoconect.view.data.repository

import com.conect.aplicativoconect.view.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun getUserData(userId: String, callback: (User) -> Unit) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val user = document.toObject(User::class.java)
                    callback(user ?: User()) // Passa o usuário ou um novo User se não encontrado
                }
            }
            .addOnFailureListener { exception ->
                // Trate erros aqui
            }
    }
}
