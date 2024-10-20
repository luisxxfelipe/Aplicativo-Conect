package com.conect.aplicativoconect.view.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var clientButton: Button
    private lateinit var businessButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection) // Altere para o layout correto

        // Referências aos botões
        clientButton = findViewById(R.id.clientButton)
        businessButton = findViewById(R.id.businessButton)

        // Definindo o listener para o botão do cliente
        clientButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                putExtra("USER_TYPE", "client") // Adiciona um extra para indicar que é um cliente
            }
            startActivity(intent)
        }

        // Definindo o listener para o botão do negócio
        businessButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                putExtra("USER_TYPE", "business") // Adiciona um extra para indicar que é uma empresa
            }
            startActivity(intent)
        }
    }
}
