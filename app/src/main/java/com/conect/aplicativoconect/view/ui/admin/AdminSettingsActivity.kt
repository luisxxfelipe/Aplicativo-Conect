package com.conect.aplicativoconect.view.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.DataBindingUtil.setContentView
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth

class AdminSettingsActivity : AppCompatActivity() {

    private lateinit var switchPushNotifications: Switch
    private lateinit var switchEmailNotifications: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        switchPushNotifications = findViewById(R.id.switchPushNotifications)
        switchEmailNotifications = findViewById(R.id.switchEmailNotifications)

        switchPushNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Notificações push ativadas", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notificações push desativadas", Toast.LENGTH_SHORT).show()
            }
        }

        switchEmailNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Notificações por email ativadas", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notificações por email desativadas", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.logoutButton)
            .setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(this, "Você saiu da conta", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}