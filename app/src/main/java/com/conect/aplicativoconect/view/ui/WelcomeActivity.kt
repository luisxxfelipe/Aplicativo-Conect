package com.conect.aplicativoconect.view.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.auth.RoleSelectionActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class WelcomeActivity : AppCompatActivity() {

    private var currentPage = 0
    private val layouts = arrayOf(
        R.layout.activity_welcome1,
        R.layout.activity_welcome2,
        R.layout.activity_welcome3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layouts[currentPage])

        requestNotificationPermission()  // Solicita a permissão assim que o app inicia
        updateButton()
        getFCMToken()
    }

    private fun updateButton() {
        val nextButton = when (currentPage) {
            0 -> findViewById<Button>(R.id.nextButton1)
            1 -> findViewById<Button>(R.id.nextButton2)
            else -> findViewById<Button>(R.id.nextButton3)
        }

        nextButton.text = if (currentPage == layouts.size - 1) "Ir para Seleção" else "Próximo"

        nextButton.setOnClickListener {
            if (currentPage < layouts.size - 1) {
                currentPage++
                setContentView(layouts[currentPage])
                updateButton()
            } else {
                navigateToRoleSelection()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Solicita a permissão
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "Permissão de notificações concedida.")
            } else {
                Log.d("Permission", "Permissão de notificações negada.")
            }
            navigateToRoleSelection()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun navigateToRoleSelection() {
        val intent = Intent(this, RoleSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val newToken = task.result
                Log.d("FCM", "Token atualizado: $newToken")
                saveTokenToFirestore(newToken)
            } else {
                Log.w("FCM", "Falha ao obter token", task.exception)
            }
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        userRef.update("fcmToken", token)
            .addOnSuccessListener { Log.d("Firestore", "Token salvo com sucesso") }
            .addOnFailureListener { e -> Log.e("Firestore", "Erro ao salvar token", e) }
    }
}
