package com.conect.aplicativoconect.ui.activities

import android.Manifest
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
    
    // 🎯 CÓDIGOS DE PERMISSÃO
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 100
        private const val LOCATION_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layouts[currentPage])

        requestPermissionsSequentially()  // 🎯 Solicita permissões sequencialmente
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

    // 🎯 SOLICITAR PERMISSÕES SEQUENCIALMENTE
    private fun requestPermissionsSequentially() {
        // Primeiro solicitar notificações
        requestNotificationPermission()
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Solicita a permissão de notificações
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_CODE
                )
            } else {
                // Se já tem permissão de notificações, pedir localização
                requestLocationPermission()
            }
        } else {
            // Android < 13 não precisa de permissão de notificações, pedir só localização
            requestLocationPermission()
        }
    }
    
    // 🆕 NOVA FUNÇÃO: Solicitar permissão de localização
    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Solicita a permissão de localização
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE
            )
        } else {
            Log.d("Permission", "✅ Permissão de localização já concedida")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            NOTIFICATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "✅ Permissão de notificações concedida")
                } else {
                    Log.d("Permission", "❌ Permissão de notificações negada")
                }
                // Após responder sobre notificações, pedir localização
                requestLocationPermission()
            }
            
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "✅ Permissão de localização concedida")
                } else {
                    Log.d("Permission", "❌ Permissão de localização negada - app funcionará sem filtros de proximidade")
                }
                // Permissões finalizadas - nada mais a fazer
            }
            
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun navigateToRoleSelection() {
        val intent = Intent(this, RoleSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }
}
