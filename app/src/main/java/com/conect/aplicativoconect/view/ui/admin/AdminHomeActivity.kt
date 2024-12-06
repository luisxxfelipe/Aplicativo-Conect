package com.conect.aplicativoconect.view.ui.admin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.UpcomingBookingWorker
import com.conect.aplicativoconect.view.ui.WelcomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
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

        testWorkerExecution()  // Apenas para teste

        setupBottomNavigation()
    }

    private fun fetchCompanyIdAndCheckSubscription() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("subscriptions")
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val document = documents.documents[0]
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

                        if (daysUntil(endDate) <= 2) {
                            Log.d(
                                "SubscriptionCheck",
                                "Conditions met for subscription expiry alert."
                            )
                            showSubscriptionExpiryAlert(endDate)
                        } else {
                            Log.d(
                                "SubscriptionCheck",
                                "No alert needed: isActive=$isActive, daysUntilEndDate=${
                                    daysUntil(endDate)
                                }"
                            )
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

    private fun testWorkerExecution() {
        val testWorkRequest = OneTimeWorkRequestBuilder<UpcomingBookingWorker>().build()
        WorkManager.getInstance(this).enqueue(testWorkRequest)
        Log.d("AdminHomeActivity", "Enfileirando execução de teste do worker.")
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

        if (lastAlertDay != today) {
            sharedPreferences.edit().putInt("dailyAlertCount", 0).putInt("lastAlertDay", today)
                .apply()
        }

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
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.roxo)) // Cor roxa para o texto

            sharedPreferences.edit().putInt("dailyAlertCount", dailyAlertCount + 1).apply()
            Log.d("SubscriptionCheck", "Alert shown, count updated to: ${dailyAlertCount + 1}")
        }
        else {
            Log.d("SubscriptionCheck", "Alert not shown due to daily limit.")
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
