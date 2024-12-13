package com.conect.aplicativoconect.view.ui.admin

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.WelcomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.androidbrowserhelper.playbilling.provider.PaymentActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdminHomeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private var companyId: String? = null  // Armazena o companyId
    private val MAX_DAILY_ALERTS = 2  // Limite de exibições do alerta por dia

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)

        firestore = FirebaseFirestore.getInstance()

        fetchCompanyIdAndCheckSubscription()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminHomeFragment())
                .commit()
        }

        setupBottomNavigation()
    }

    private fun fetchCompanyIdAndCheckSubscription() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("subscriptions")
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) { // Verifique se a consulta retornou documentos
                        val document = querySnapshot.documents[0]
                        companyId = document.id
                        val endDate = document.getTimestamp("endDate")
                        val isActive = document.getBoolean("isActive") ?: false
                        Log.d("SubscriptionCheck", "isActive: $isActive, endDate: $endDate")

                        if (endDate == null) {
                            Log.d(
                                "SubscriptionCheck",
                                "End date is missing for subscription ID: $companyId"
                            )
                            Toast.makeText(
                                this,
                                "Data de término da assinatura ausente. Verifique no Firebase.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addOnSuccessListener
                        }

                        // Verifica se a assinatura está ativa e se falta pouco para expirar
                        if (!isActive || daysUntil(endDate) <= 2) {
                            showSubscriptionExpiryAlert(endDate) // Exibe alerta de expiração se necessário
                        }

                        // Se assinatura estiver expirada ou não ativa, podemos redirecionar o usuário
                        if (!isActive) {
                            Toast.makeText(
                                this,
                                "Sua assinatura expirou. Por favor, renove sua assinatura.",
                                Toast.LENGTH_LONG
                            ).show()
                            // Redirecionar para a tela de pagamento ou renovação da assinatura
                            startActivity(Intent(this, PaymentActivity::class.java))
                            finish()
                        }

                    } else {
                        Toast.makeText(
                            this,
                            "Assinatura não encontrada para este usuário.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao buscar assinatura.", Toast.LENGTH_SHORT).show()
                }
        }
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
            val endDateFormatted =
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(endDate.toDate())

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
            Log.d("SubscriptionCheck", "Alert shown, count updated to: ${dailyAlertCount + 1}")
        } else {
            Log.d("SubscriptionCheck", "Alert not shown due to daily limit.")
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
        val dialog = AddServiceDialogFragment()
        val args = Bundle().apply { putString("companyId", companyId) }
        dialog.arguments = args
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
}
