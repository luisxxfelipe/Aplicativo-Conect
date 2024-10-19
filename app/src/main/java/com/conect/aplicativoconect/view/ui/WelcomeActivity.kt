package com.conect.aplicativoconect.view.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.auth.LoginActivity

class WelcomeActivity : AppCompatActivity() {
    private var currentPage = 0
    private val layouts = arrayOf(R.layout.activity_welcome1, R.layout.activity_welcome2, R.layout.activity_welcome3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layouts[currentPage])

        updateButton()
    }

    private fun updateButton() {
        // Atualizar o layout e encontrar a referência do botão
        val nextButton = when (currentPage) {
            0 -> findViewById<Button>(R.id.nextButton1)
            1 -> findViewById<Button>(R.id.nextButton2)
            else -> findViewById<Button>(R.id.nextButton3)
        }

        nextButton.text = if (currentPage == layouts.size - 1) "Ir para Login" else "Próximo"

        // Listener do botão
        nextButton.setOnClickListener {
            if (currentPage < layouts.size - 1) {
                currentPage++
                setContentView(layouts[currentPage])
                updateButton() // Atualiza a referência e o texto do botão
            } else {
                // Redirecionar para a tela de login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
