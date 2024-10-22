package com.conect.aplicativoconect.view.ui.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.ui.WelcomeActivity
import com.conect.aplicativoconect.view.ui.admin.BookingAdapter
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClienteHomeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private val clientViewModel: ClientViewModel by viewModels() // Inicializa o ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_home)

        firestore = FirebaseFirestore.getInstance() // Inicializa o Firestore
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(ClienteHomeFragment())
                    true
                }
                R.id.navigation_appointments -> {
                    loadFragment(BookingFragment())
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ClienteProfileFragment())
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
        loadFragment(ClienteHomeFragment())
        fetchUserName() // Chama o método para buscar o nome do usuário

        // Observe as mudanças na lista de agendamentos
        clientViewModel.todayBookings.observe(this) { bookings ->
            updateBookingsView(bookings)
        }
    }

    private fun loadFragment(fragment: Fragment, userName: String? = null) {
        if (fragment is ClienteHomeFragment) {
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
                            clientViewModel.setUserName(userName ?: "Nome não encontrado") // Atualiza o nome no ViewModel
                            // Carregue o HomeFragment e passe o nome do usuário
                            loadFragment(ClienteHomeFragment(), userName) // Passando o nome do usuário
                            fetchTodayBookings(userEmail) // Chama o método para buscar agendamentos
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("ClientHomeActivity", "Erro ao buscar nome do usuário", e)
                    }
            }
        }
    }

    private fun fetchTodayBookings(userEmail: String) {
        firestore.collection("bookings")
            .whereEqualTo("userEmail", userEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // Se não houver agendamentos, exiba a mensagem e a imagem
                    clientViewModel.setTodayBookings(emptyList())
                } else {
                    val bookings = querySnapshot.toObjects(Booking::class.java)
                    clientViewModel.setTodayBookings(bookings)
                }
            }
            .addOnFailureListener { e ->
                Log.w("ClientHomeActivity", "Erro ao buscar agendamentos", e)
            }
    }

    private fun updateBookingsView(bookings: List<Booking>) {
        val noBookingsMessage = findViewById<TextView>(R.id.noBookingsMessage)
        val noBookingsImage = findViewById<ImageView>(R.id.noBookingsImage)
        val recyclerView = findViewById<RecyclerView>(R.id.todayBookingsRecyclerView)

        if (bookings.isEmpty()) {
            noBookingsMessage.visibility = View.VISIBLE
            noBookingsImage.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noBookingsMessage.visibility = View.GONE
            noBookingsImage.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            // Aqui você deve configurar seu adapter e definir os dados
            val adapter = BookingAdapter(bookings)
            recyclerView.adapter = adapter
            // Defina um layout manager se necessário
            recyclerView.layoutManager = LinearLayoutManager(this)
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
