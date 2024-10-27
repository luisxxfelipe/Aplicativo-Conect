package com.conect.aplicativoconect.view.ui

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()
        // Força a obtenção do token ao criar o serviço
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "Token obtido: $token")
                saveTokenToFirestore(token)
            } else {
                Log.e("FCM", "Erro ao obter token", task.exception)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Novo token gerado: $token")

        // Salva o token diretamente no Firestore
        saveTokenToFirestore(token)
    }



    private fun saveTokenToFirestore(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val userId = currentUser.uid

            // Verifica se é um usuário comum
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Log.d("FCM", "Usuário encontrado: ${document.getString("name")}")
                        updateTokenInFirestore("users", userId, token)
                    } else {
                        // Se não for usuário, verifica se é uma empresa
                        firestore.collection("business").document(userId).get()
                            .addOnSuccessListener { businessDocument ->
                                if (businessDocument.exists()) {
                                    Log.d("FCM", "Empresa encontrada: ${businessDocument.getString("name")}")
                                    updateTokenInFirestore("business", userId, token)
                                } else {
                                    Log.e("FCM", "Usuário ou empresa não encontrado no Firestore.")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("FCM", "Erro ao buscar empresa: ${e.message}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Erro ao buscar usuário: ${e.message}")
                }
        } else {
            Log.e("FCM", "Nenhum usuário autenticado.")
        }
    }



    // Função genérica para atualizar o token no Firestore
    private fun updateTokenInFirestore(collection: String, userId: String, token: String) {
        firestore.collection(collection).document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM", "Token salvo com sucesso na coleção '$collection' para o usuário $userId.")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Erro ao salvar token: ${e.message}")
            }
    }



    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Mensagem recebida de: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Payload da mensagem: ${remoteMessage.data}")
        }

        remoteMessage.notification?.let {
            Log.d("FCM", "Corpo da notificação: ${it.body}")
        }
    }
}
