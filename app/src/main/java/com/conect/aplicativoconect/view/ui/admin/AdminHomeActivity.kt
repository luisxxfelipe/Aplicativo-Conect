package com.conect.aplicativoconect.view.ui.admin

import AdminProfileFragment
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.WelcomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class AdminHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)

        // Carregar o fragmento de home do administrador ao iniciar
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminHomeFragment())
                .commit()
        }

        // Configuração do BottomNavigationView
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AdminHomeFragment())
                        .commit()
                    true
                }
                R.id.navigation_appointments -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AdminBookingsFragment())
                        .commit()
                    true
                }
                R.id.navigation_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AdminProfileFragment())
                        .commit()
                    true
                }
                R.id.navigation_logout -> {
                    logout() // Chamar o método de logout
                    true
                }
                else -> false
            }
        }
    }

    private fun logout() {
        // Fazer logout do Firebase
        FirebaseAuth.getInstance().signOut()

        // Voltar para a tela de boas-vindas
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpar a pilha de atividades
        startActivity(intent)
        finish() // Finaliza a AdminHomeActivity
    }
}
