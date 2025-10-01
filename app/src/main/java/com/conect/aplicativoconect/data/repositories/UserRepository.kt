package com.conect.aplicativoconect.data.repositories

import com.conect.aplicativoconect.data.models.User
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getUserData(userId: String, callback: (User?) -> Unit) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    callback(user)
                } else {
                    callback(null) // Caso o documento não exista
                }
            }
            .addOnFailureListener {
                callback(null) // Tratar falha conforme necessário
            }
    }
}
