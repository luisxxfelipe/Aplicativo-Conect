package com.conect.aplicativoconect.view.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.WelcomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminHomeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private var companyId: String? = null  // Armazena o companyId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)

        firestore = FirebaseFirestore.getInstance()

        // Buscar o companyId do administrador logado
        fetchCompanyId()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminHomeFragment())
                .commit()
        }

        setupBottomNavigation()
    }

    private fun fetchCompanyId() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("business")
                .whereEqualTo("ownerId", userId)  // Alterado para buscar pelo campo "ownerId"
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        companyId = documents.documents[0].id  // Armazena o ID da empresa
                    } else {
                        Toast.makeText(this, "Empresa não encontrada para este usuário.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao buscar empresa.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(AdminHomeFragment())
                    true
                }
                R.id.navigation_appointments -> {
                    loadFragment(AdminBookingsFragment())
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(AdminProfileFragment())
                    true
                }
                R.id.navigation_add_service -> {
                    showAddServiceDialog()
                    true
                }
                R.id.navigation_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showAddServiceDialog() {
        val dialog = AddServiceDialogFragment()
        val args = Bundle().apply {
            putString("companyId", companyId)  // Adiciona o companyId aos argumentos
        }
        dialog.arguments = args
        dialog.show(supportFragmentManager, "AddServiceDialog")
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmar Logout")
            .setMessage("Você tem certeza que deseja sair?")
            .setPositiveButton("Sim") { _, _ -> logout() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
