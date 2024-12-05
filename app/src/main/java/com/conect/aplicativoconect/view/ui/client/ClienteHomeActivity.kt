package com.conect.aplicativoconect.view.ui.client

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.ui.WelcomeActivity
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClienteHomeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private val clientViewModel: ClientViewModel by viewModels()
    private lateinit var clientBookingAdapter: ClientBookingAdapter
    private var loadingDialog: Dialog? = null
    private var isFromSignup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_home)

        firestore = FirebaseFirestore.getInstance()
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Verificação de autenticação
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            redirectToLogin() // Redireciona para tela de login se o usuário não estiver autenticado
            return
        }

        // Check if the user came from the signup screen
        isFromSignup = intent.getBooleanExtra("FROM_SIGNUP", false)

        // Setup the loading dialog
        setupLoadingDialog()

        // Show the loading dialog only if coming from signup
        if (isFromSignup) {
            showLoadingDialogWithDelay()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
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
                    showLogoutConfirmationDialog()
                    true
                }
                else -> false
            }
        }

        loadFragment(ClienteHomeFragment())
        fetchUserName()

        clientViewModel.todayBookings.observe(this) { bookings ->
            updateBookingsView(bookings)
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }

    private fun setupLoadingDialog() {
        loadingDialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        loadingDialog?.setContentView(view)
        loadingDialog?.setCancelable(false)
        loadingDialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showLoadingDialogWithDelay() {
        showLoadingDialog()
        // Keep the dialog open for 3 seconds (3000 milliseconds)
        Handler(Looper.getMainLooper()).postDelayed({
            hideLoadingDialog()
        }, 3000)
    }

    private fun showLoadingDialog() {
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private fun loadFragment(fragment: Fragment, userName: String? = null) {
        if (fragment is ClienteHomeFragment) {
            val bundle = Bundle()
            bundle.putString("userName", userName)
            fragment.arguments = bundle
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        Log.d("ClientHomeActivity", "Fragment ${fragment.javaClass.simpleName} carregado.")
    }

    private fun fetchUserName() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.email?.let { userEmail ->
            firestore.collection("users").whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val userName = querySnapshot.documents[0].getString("name") ?: "Nome não encontrado"
                        clientViewModel.loadUserData(user.uid)
                        loadFragment(ClienteHomeFragment(), userName)
                        fetchTodayBookings(user.uid)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("ClientHomeActivity", "Erro ao buscar nome do usuário", e)
                }
        }
    }

    private fun fetchTodayBookings(userId: String) {
        clientViewModel.fetchUserBookings(userId)
    }

    private fun updateBookingsView(bookings: List<Booking>) {
        val noBookingsMessage = findViewById<TextView>(R.id.noBookingsMessage)
        val noBookingsImage = findViewById<ImageView>(R.id.noBookingsImage)
        val recyclerView = findViewById<RecyclerView>(R.id.todayBookingsRecyclerView)

        if (noBookingsMessage == null || noBookingsImage == null || recyclerView == null) {
            Log.e("ClienteHomeActivity", "Views de agendamentos não foram encontradas.")
            return
        }

        if (bookings.isEmpty()) {
            noBookingsMessage.visibility = View.VISIBLE
            noBookingsImage.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noBookingsMessage.visibility = View.GONE
            noBookingsImage.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            if (!::clientBookingAdapter.isInitialized) {
                clientBookingAdapter = ClientBookingAdapter(
                    context = this,
                    bookings = bookings,
                    onConfirmClick = { booking -> clientViewModel.confirmBooking(this, booking) },
                    onCancelClick = { booking -> clientViewModel.cancelBooking(this, booking) },
                    onRateClick = { booking -> showRatingPopup(booking) },
                    onEmptyList = { showNoBookingsMessage() }
                )
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.adapter = clientBookingAdapter
            } else {
                clientBookingAdapter.updateData(bookings)
            }
        }
    }

    private fun showNoBookingsMessage() {
        findViewById<TextView>(R.id.noBookingsMessage)?.visibility = View.VISIBLE
        findViewById<ImageView>(R.id.noBookingsImage)?.visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.todayBookingsRecyclerView)?.visibility = View.GONE
    }

    private fun showRatingPopup(booking: Booking) {
        val dialogView = layoutInflater.inflate(R.layout.popup_rating, null)
        val ratingQuality = dialogView.findViewById<RatingBar>(R.id.ratingQuality)
        val ratingPunctuality = dialogView.findViewById<RatingBar>(R.id.ratingPunctuality)
        val ratingService = dialogView.findViewById<RatingBar>(R.id.ratingService)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        saveButton.setOnClickListener {
            val qualityRating = ratingQuality.rating.toInt()
            val punctualityRating = ratingPunctuality.rating.toInt()
            val serviceRating = ratingService.rating.toInt()
            clientViewModel.saveRatings(this, booking.id, qualityRating, punctualityRating, serviceRating)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
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
