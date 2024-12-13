package com.conect.aplicativoconect.view.ui.client

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

        isFromSignup = intent.getBooleanExtra("FROM_SIGNUP", false)
        setupLoadingDialog()
        if (isFromSignup) {
            showLoadingDialogWithDelay()
        }

        // Configurar o título inicial do menu
        updateActionBarTitle(R.id.navigation_home)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    updateActionBarTitle(R.id.navigation_home)
                    loadFragment(ClienteHomeFragment())
                    true
                }

                R.id.navigation_appointments -> {
                    updateActionBarTitle(R.id.navigation_appointments)
                    loadFragment(BookingFragment())
                    true
                }

                R.id.navigation_profile -> {
                    updateActionBarTitle(R.id.navigation_profile)
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
        // Definindo o bundle, se necessário
        if (fragment is ClienteHomeFragment) {
            val bundle = Bundle()
            bundle.putString("userName", userName)
            fragment.arguments = bundle
        }

        val transaction = supportFragmentManager.beginTransaction()

        // Verifique se o fragmento atual é o que está sendo mostrado
        val isFragmentBackNavigation = supportFragmentManager.backStackEntryCount > 0

        // Se estiver voltando, adicione animação de transição
        if (isFragmentBackNavigation) {
            transaction.setCustomAnimations(
                android.R.anim.slide_in_left,  // Novo fragmento entrando da esquerda
                android.R.anim.slide_out_right // Fragmento atual saindo para a direita
            )
        } else {
            // Caso contrário, não faz animação (navegação para frente)
            transaction.setCustomAnimations(0, 0)
        }

        // Substitui o fragmento
        transaction.replace(R.id.fragment_container, fragment)
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
                        val userName =
                            querySnapshot.documents[0].getString("name") ?: "Nome não encontrado"
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
                    context = this,  // Aqui você está passando o contexto correto
                    bookings = bookings,
                    onConfirmClick = { booking ->
                        clientViewModel.confirmBooking(
                            booking,
                            this
                        )
                    }, // Passando o contexto
                    onCancelClick = { booking ->
                        clientViewModel.cancelBooking(
                            booking,
                            this
                        )
                    }, // Passando o contexto
                    onEmptyList = { showNoBookingsMessage() }  // Mantido apenas a funcionalidade de "empty list"
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

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
