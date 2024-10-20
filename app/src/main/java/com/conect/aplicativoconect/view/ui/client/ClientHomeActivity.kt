package com.conect.aplicativoconect.view.ui.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.WelcomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ClientHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_home)

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())  // Crie um fragmento HomeFragment
                    true
                }
                R.id.navigation_appointments -> {
                    loadFragment(BookingFragment())  // Crie um fragmento AppointmentsFragment
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment())  // Crie um fragmento ProfileFragment
                    true
                }
                R.id.navigation_logout -> {
                    logout() // Chamar o método de logout
                    true
                }
                else -> false
            }
        }

        // Carregue o fragmento inicial
        loadFragment(HomeFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
        Log.d("com.conect.aplicativoconect.view.ui.client.ClientHomeActivity", "Fragment ${fragment.javaClass.simpleName} carregado.")
    }

    private fun logout() {
        // Fazer logout do Firebase
        FirebaseAuth.getInstance().signOut()

        // Voltar para a tela de boas-vindas
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpar a pilha de atividades
        startActivity(intent)
        finish() // Finaliza a ClientHomeActivity
    }
}
