package com.conect.aplicativoconect.view.ui.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.WelcomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClientHomeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_home)

        firestore = FirebaseFirestore.getInstance() // Inicializa o Firestore
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.navigation_appointments -> {
                    loadFragment(BookingFragment())
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                R.id.navigation_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        // Carregue o fragmento inicial
        loadFragment(HomeFragment())
        fetchUserName() // Chama o método para buscar o nome do usuário
    }

    private fun loadFragment(fragment: Fragment, userName: String? = null) {
        if (fragment is HomeFragment) {
            val bundle = Bundle()
            bundle.putString("userName", userName)
            fragment.arguments = bundle
        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
        Log.d("ClientHomeActivity", "Fragment ${fragment.javaClass.simpleName} carregado.")
    }


    private fun fetchUserName() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val userEmail = it.email
            if (userEmail != null) {
                firestore.collection("users").whereEqualTo("email", userEmail)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val userDocument = querySnapshot.documents[0]
                            val userName = userDocument.getString("name")
                            // Carregue o HomeFragment e passe o nome do usuário
                            loadFragment(HomeFragment(), userName) // Passando o nome do usuário
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("ClientHomeActivity", "Erro ao buscar nome do usuário", e)
                    }
            }
        }
    }


    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


}
