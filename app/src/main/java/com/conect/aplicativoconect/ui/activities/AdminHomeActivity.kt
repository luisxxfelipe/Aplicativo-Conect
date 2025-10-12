package com.conect.aplicativoconect.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.ui.fragments.AddServiceDialogFragment
import com.conect.aplicativoconect.ui.fragments.AdminBookingsFragment
import com.conect.aplicativoconect.ui.fragments.AdminHomeFragment
import com.conect.aplicativoconect.ui.fragments.AdminProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.conect.aplicativoconect.utils.Validator
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AdminHomeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private val MAX_DAILY_ALERTS = 2  // Limite de exibições do alerta por dia
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)

        firestore = FirebaseFirestore.getInstance()
        checkLocationPermission()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminHomeFragment())
                .commit()
        }

        setupBottomNavigation()
    }

    private fun daysUntil(endDate: Timestamp): Long {
        val currentDate = Calendar.getInstance().time
        val endDateMillis = endDate.toDate().time
        val diffMillis = endDateMillis - currentDate.time
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    private fun showSubscriptionExpiryAlert(endDate: Timestamp) {
        val sharedPreferences = getSharedPreferences("SubscriptionPrefs", Context.MODE_PRIVATE)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastAlertDay = sharedPreferences.getInt("lastAlertDay", -1)
        val dailyAlertCount = sharedPreferences.getInt("dailyAlertCount", 0)

        // Se o dia mudou, resetamos a contagem de alertas
        if (lastAlertDay != today) {
            sharedPreferences.edit().putInt("dailyAlertCount", 0).putInt("lastAlertDay", today)
                .apply()
        }

        // Se a quantidade de alertas diários for menor que o máximo, mostramos o alerta
        if (dailyAlertCount < MAX_DAILY_ALERTS) {
            val endDateFormatted = Validator.DATE_FORMAT.format(endDate.toDate()) // ✅ OTIMIZADO: Formatador central

            val dialog = AlertDialog.Builder(this)
                .setTitle("Sua assinatura está quase vencendo!")
                .setMessage("Sua assinatura vencerá em breve em $endDateFormatted. Garanta sua renovação para continuar aproveitando os serviços.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .create()

            dialog.show()

            // Acessando o botão "OK" e alterando a cor do texto para roxo
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.roxo
                )
            ) // Cor roxa para o texto

            // Atualizando a contagem de alertas diários
            sharedPreferences.edit().putInt("dailyAlertCount", dailyAlertCount + 1).apply()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Configurar as cores para os ícones e textos selecionados
        val selectedColor =
            ContextCompat.getColor(this, R.color.roxo) // Cor roxa para ícones selecionados
        val unselectedColor =
            ContextCompat.getColor(this, R.color.cinza_escuro) // Cor para ícones não selecionados

        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),  // Item selecionado
                intArrayOf(-android.R.attr.state_selected)  // Item não selecionado
            ),
            intArrayOf(
                selectedColor,
                unselectedColor
            )  // Cor para ícone selecionado e não selecionado
        )

        // Aplicar a cor nos ícones e no texto dos itens
        bottomNavigation.itemIconTintList = colorStateList
        bottomNavigation.itemTextColor = colorStateList

        // Configurar o título inicial do menu
        updateActionBarTitle(R.id.navigation_home)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    updateActionBarTitle(R.id.navigation_home)
                    loadFragment(AdminHomeFragment())
                    true
                }

                R.id.navigation_appointments -> {
                    updateActionBarTitle(R.id.navigation_appointments)
                    loadFragment(AdminBookingsFragment())
                    true
                }

                R.id.navigation_profile -> {
                    updateActionBarTitle(R.id.navigation_profile)
                    loadFragment(AdminProfileFragment())
                    true
                }

                R.id.navigation_add_service -> {
                    updateActionBarTitle(R.id.navigation_add_service)
                    showAddServiceDialog()
                    true
                }

                R.id.navigation_logout -> {
                    updateActionBarTitle(R.id.navigation_logout)
                    showLogoutConfirmationDialog()
                    true
                }

                else -> false
            }
        }
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

    private fun showAddServiceDialog() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val dialog = AddServiceDialogFragment().apply {
            arguments = Bundle().apply {
                putString("companyId", userId)
            }
        }
        dialog.show(supportFragmentManager, "AddServiceDialog")
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
            .setTitle("Confirmar Logout")
            .setMessage("Você tem certeza que deseja sair?")
            .setPositiveButton("Sim") { _, _ -> logout() }
            .setNegativeButton("Cancelar", null)

        // Criação do dialog
        val dialog = builder.create()

        // Exibindo o dialog
        dialog.show()

        // Acessando os botões e alterando suas cores
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        positiveButton.setTextColor(ContextCompat.getColor(this, android.R.color.black))

        negativeButton.setTextColor(ContextCompat.getColor(this, android.R.color.black))
    }


    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, WelcomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    // Verifica e solicita permissão de localização
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
            .setTitle("Permissão de Localização")
            .setMessage("O Conectx usa sua localização para melhorar os serviços oferecidos e conectar você com clientes próximos.")
            .setPositiveButton("Permitir") { _, _ ->
                requestLocationPermission()
            }
            .setNegativeButton("Agora Não", null)
            .setCancelable(true)
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
                    // Permissão concedida
                } else {
                    // Permissão negada - não é crítica para empresas
                }
            }
        }
    }

    // Intercepta o botão voltar para evitar retorno indesejado à tela de seleção
    override fun onBackPressed() {
        // Mostrar dialog de confirmação para sair do app
        AlertDialog.Builder(this)
            .setTitle("Sair do Aplicativo")
            .setMessage("Deseja realmente sair do Conectx?")
            .setPositiveButton("Sim") { _, _ ->
                // Fecha completamente o aplicativo
                finishAffinity()
            }
            .setNegativeButton("Não", null)
            .show()
    }
}
