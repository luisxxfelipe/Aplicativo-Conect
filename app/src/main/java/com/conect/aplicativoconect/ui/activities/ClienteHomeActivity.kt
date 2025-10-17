package com.conect.aplicativoconect.ui.activities

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Booking
import com.conect.aplicativoconect.ui.adapters.ClientBookingAdapter
import com.conect.aplicativoconect.ui.fragments.BookingFragment
import com.conect.aplicativoconect.ui.fragments.ClienteHomeFragment
import com.conect.aplicativoconect.ui.fragments.ClienteProfileFragment
import com.conect.aplicativoconect.ui.viewmodels.ClientViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClienteHomeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private val clientViewModel: ClientViewModel by viewModels()
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    private val clientBookingAdapter: ClientBookingAdapter by lazy {
        ClientBookingAdapter(
            context = this,
            bookings = mutableListOf(),
            onConfirmClick = { booking ->
                clientViewModel.confirmBooking(booking, this)
            },
            onCancelClick = { booking ->
                clientViewModel.cancelBooking(booking, this)
            },
            onEmptyList = { showNoBookingsMessage() },
            onCardClick = { companyId -> openCompanyDetails(companyId) }
        )
    }
    private var loadingDialog: Dialog? = null
    private var isFromSignup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_home)

        firestore = FirebaseFirestore.getInstance()
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        checkAuthentication()
        checkLocationPermission()

        isFromSignup = intent.getBooleanExtra("FROM_SIGNUP", false)
        setupLoadingDialog()
        // ✅ OTIMIZAÇÃO: Removido delay desnecessário de 3 segundos
        // Loading será controlado pelos fragments conforme necessário


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
        clientViewModel.loadUserData(FirebaseAuth.getInstance().currentUser?.uid ?: "")
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

    // ✅ FUNÇÃO REMOVIDA: showLoadingDialogWithDelay era desnecessária
    // Loading agora é controlado dinamicamente pelos dados reais

    private fun showLoadingDialog() {
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private fun loadFragment(fragment: Fragment, args: Bundle? = null) {
        args?.let { fragment.arguments = it }

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val existingFragment =
            supportFragmentManager.findFragmentByTag(fragment.javaClass.simpleName)

        supportFragmentManager.fragments.forEach { frag ->
            fragmentTransaction.hide(frag)
        }

        if (existingFragment != null) {
            fragmentTransaction.show(existingFragment)
        } else {
            fragmentTransaction.add(
                R.id.fragment_container,
                fragment,
                fragment.javaClass.simpleName
            )
        }

        fragmentTransaction.commit()
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

            // clientBookingAdapter já está inicializado com by lazy - apenas configurar adapter e atualizar dados
            recyclerView?.layoutManager = LinearLayoutManager(this)
            recyclerView?.adapter = clientBookingAdapter
            clientBookingAdapter.updateData(bookings)
        }
    }

    private fun openCompanyDetails(companyId: String) {
        val intent = Intent(this, EmpresaDetalhesActivity::class.java).apply {
            putExtra("companyId", companyId)
        }
        startActivity(intent)
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

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        clearLocalCache() // Limpa o cache local

        // Verificar se há userType salvo para ir direto para Login
        val sharedPref = getSharedPreferences("userTypePrefs", MODE_PRIVATE)
        val savedUserType = sharedPref.getString("USER_TYPE", null)

        val intent = if (savedUserType != null) {
            Intent(this, LoginActivity::class.java).apply {
                putExtra("USER_TYPE", savedUserType)
            }
        } else {
            Intent(this, WelcomeActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun clearLocalCache() {
        getSharedPreferences("business_cache", MODE_PRIVATE).edit().clear().apply()
    }

    // Verifica e solicita permissão de localização obrigatória
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permissão já concedida
                return
            }
            
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // Mostrar explicação sobre porque a permissão é necessária
                showLocationPermissionExplanation()
            }
            
            else -> {
                // Solicitar permissão diretamente
                requestLocationPermission()
            }
        }
    }

    private fun showLocationPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Permissão de Localização Necessária")
            .setMessage("O Conectex precisa acessar sua localização para mostrar estabelecimentos próximos a você e melhorar sua experiência.")
            .setPositiveButton("Permitir") { _, _ ->
                requestLocationPermission()
            }
            .setNegativeButton("Agora Não") { _, _ ->
                // Usuario pode continuar sem localização, mas com funcionalidade limitada
            }
            .setCancelable(false)
            .show()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permissão concedida - o fragment já irá detectar automaticamente
                } else {
                    // Permissão negada - informar o usuário
                    AlertDialog.Builder(this)
                        .setTitle("Permissão Negada")
                        .setMessage("Sem a permissão de localização, você verá todos os estabelecimentos, mas não conseguirá filtrar por proximidade.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    // Intercepta o botão voltar para evitar retorno indesejado à tela de seleção
    override fun onBackPressed() {
        // Mostrar dialog de confirmação para sair do app
        AlertDialog.Builder(this)
            .setTitle("Sair do Aplicativo")
            .setMessage("Deseja realmente sair do Conectex?")
            .setPositiveButton("Sim") { _, _ ->
                // Fecha completamente o aplicativo
                finishAffinity()
            }
            .setNegativeButton("Não", null)
            .show()
    }

}
