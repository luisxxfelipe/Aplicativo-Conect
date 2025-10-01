package com.conect.aplicativoconect.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.conect.aplicativoconect.R

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var clientCard: LinearLayout
    private lateinit var businessCard: LinearLayout
    private lateinit var clientIcon: ImageView
    private lateinit var clientText: TextView
    private lateinit var businessIcon: ImageView
    private lateinit var businessText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        // Referências
        clientCard = findViewById(R.id.clientCard)
        businessCard = findViewById(R.id.businessCard)
        clientIcon = findViewById(R.id.clientIcon)
        clientText = findViewById(R.id.clientText)
        businessIcon = findViewById(R.id.businessIcon)
        businessText = findViewById(R.id.businessText)

        val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down)

        // Listener para o Card de Cliente
        clientCard.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
                updateCardAppearance(clientCard, clientIcon, clientText)
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToLogin("client")
                }, 300)
            }
            clientCard.startAnimation(scaleDown)
            true
        }

        // Listener para o Card de Empresa
        businessCard.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
                updateCardAppearance(businessCard, businessIcon, businessText)
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToLogin("business")
                }, 300)
            }
            businessCard.startAnimation(scaleDown)
            true
        }
    }

    // Função para restaurar o estado original dos cards
    override fun onResume() {
        super.onResume()
        resetCardAppearance(clientCard, clientIcon, clientText)
        resetCardAppearance(businessCard, businessIcon, businessText)
    }

    private fun updateCardAppearance(card: LinearLayout, icon: ImageView, text: TextView) {
        card.setBackgroundResource(R.drawable.card_background_selected)
        icon.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
        text.setTextColor(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun resetCardAppearance(card: LinearLayout, icon: ImageView, text: TextView) {
        card.setBackgroundResource(R.drawable.card_background_role)  // Volta para o fundo original
        icon.setColorFilter(
            ContextCompat.getColor(
                this,
                R.color.colorPrimary
            )
        ) // Cor original do ícone
        text.setTextColor(ContextCompat.getColor(this, R.color.black)) // Cor original do texto
    }

    private fun navigateToLogin(userType: String) {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("USER_TYPE", userType)
        }
        startActivity(intent)
    }
}
