package com.conect.aplicativoconect.ui.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.conect.aplicativoconect.R

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
    }

    private fun updateButton() {
        val nextButton = when (currentPage) {
            0 -> findViewById(R.id.nextButton1)
            1 -> findViewById(R.id.nextButton2)
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
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun navigateToRoleSelection() {
        val intent = Intent(this, RoleSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }
}
