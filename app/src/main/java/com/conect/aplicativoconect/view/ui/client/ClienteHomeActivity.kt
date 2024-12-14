package com.conect.aplicativoconect.view.ui.client

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.ui.WelcomeActivity
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

        checkAuthentication()

        isFromSignup = intent.getBooleanExtra("FROM_SIGNUP", false)
        setupLoadingDialog()
        if (isFromSignup) {
            showLoadingDialogWithDelay()
        }


        // Configurar o título inicial do menu
        updateActionBarTitle(R.id.navigation_home)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> loadFragment(ClienteHomeFragment())
                R.id.navigation_appointments -> loadFragment(BookingFragment())
                R.id.navigation_profile -> loadFragment(ClienteProfileFragment())
                R.id.navigation_logout -> showLogoutConfirmationDialog()
            }
            true
        }


        loadFragment(ClienteHomeFragment())
        fetchUserName()
        clientViewModel.todayBookings.observe(this) { bookings ->
            updateBookingsView(bookings)
        }
        setBottomNavIconColors(bottomNavigation)
    }

    private fun updateActionBarTitle(itemId: Int) {
        val title = when (itemId) {
            R.id.navigation_home -> "Início"
            R.id.navigation_appointments -> "Agendamentos"
            R.id.navigation_profile -> "Perfil"
            else -> "Conectex"
        }
        supportActionBar?.title = title
    }

    private fun setBottomNavIconColors(bottomNavigation: BottomNavigationView) {
        val selectedColor =
            ContextCompat.getColor(this, R.color.roxo) // Cor roxa para ícones selecionados
        val unselectedColor =
            ContextCompat.getColor(this, R.color.cinza_escuro) // Cor para ícones não selecionados

        // Criar o ColorStateList para ícones selecionados e não selecionados
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected), // Item selecionado
                intArrayOf(-android.R.attr.state_selected) // Item não selecionado
            ),
            intArrayOf(selectedColor, unselectedColor) // Cor do ícone selecionado e não selecionado
        )

        // Aplicar o ColorStateList tanto nos ícones quanto no texto dos itens
        bottomNavigation.itemIconTintList = colorStateList
        bottomNavigation.itemTextColor = colorStateList
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
        lifecycleScope.launch {
            delay(3000) // Aguarda 3 segundos
            hideLoadingDialog()
        }
    }

    private fun showLoadingDialog() {
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private fun loadFragment(fragment: Fragment, args: Bundle? = null) {
        args?.let { fragment.arguments = it }

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val existingFragment = supportFragmentManager.findFragmentByTag(fragment.javaClass.simpleName)

        supportFragmentManager.fragments.forEach { frag ->
            fragmentTransaction.hide(frag)
        }

        if (existingFragment != null) {
            fragmentTransaction.show(existingFragment)
        } else {
            fragmentTransaction.add(R.id.fragment_container, fragment, fragment.javaClass.simpleName)
        }

        fragmentTransaction.commit()
    }

    private fun fetchUserName() {
        lifecycleScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch
                val snapshot = withContext(Dispatchers.IO) {
                    firestore.collection("users").whereEqualTo("email", user.email).get().await()
                }
                if (!snapshot.isEmpty) {
                    val userName = snapshot.documents[0].getString("name") ?: "Nome não encontrado"
                    clientViewModel.loadUserData(user.uid)

                    // Configura os argumentos para o fragmento
                    val args = Bundle()
                    args.putString("userName", userName)

                    // Chama o fragmento com os argumentos
                    loadFragment(ClienteHomeFragment(), args)

                    fetchTodayBookings(user.uid)
                }
            } catch (e: Exception) {
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

        if (bookings.isEmpty()) {
            noBookingsMessage?.visibility = View.VISIBLE
            noBookingsImage?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
        } else {
            noBookingsMessage?.visibility = View.GONE
            noBookingsImage?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE

            if (!::clientBookingAdapter.isInitialized) {
                clientBookingAdapter = ClientBookingAdapter(
                    context = this,
                    bookings = bookings.toMutableList(), // Passa uma lista mutável
                    onConfirmClick = { booking ->
                        clientViewModel.confirmBooking(booking, this)
                    },
                    onCancelClick = { booking ->
                        clientViewModel.cancelBooking(booking, this)
                    },
                    onEmptyList = { showNoBookingsMessage() }
                )
                recyclerView?.layoutManager = LinearLayoutManager(this)
                recyclerView?.adapter = clientBookingAdapter
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

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        val dialog = builder.setTitle("Confirmar Logout")
            .setMessage("Você tem certeza que deseja sair?")
            .setPositiveButton("Sim") { _, _ -> logout() }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.roxo))

            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.roxo))
        }

        dialog.show()
    }

    private fun checkAuthentication() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            redirectToLogin()
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        clearLocalCache() // Limpa o cache local

        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun clearLocalCache() {
        getSharedPreferences("business_cache", MODE_PRIVATE).edit().clear().apply()
    }

}
