package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.R
import com.google.android.material.bottomnavigation.BottomNavigationView

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

}
