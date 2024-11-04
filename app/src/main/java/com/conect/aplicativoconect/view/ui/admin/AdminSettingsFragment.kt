package com.conect.aplicativoconect.view.ui.admin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.databinding.LayoutAdminSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class AdminSettingsFragment : Fragment() {

    private var _binding: LayoutAdminSettingsBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutAdminSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureNotificationSwitch()
        loadSubscriptionData()

        // Configurar a exclusão apenas dos dados (sem excluir a conta ou assinatura)
        binding.deleteAccountButton.setOnClickListener {
            showDeleteDataConfirmation()
        }
    }

    private fun configureNotificationSwitch() {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isNotificationsEnabled = sharedPreferences.getBoolean("admin_notifications_enabled", false)
        binding.notificationsSwitch.isChecked = isNotificationsEnabled

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("admin_notifications_enabled", isChecked)
            editor.apply()

            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("admin_users")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Notificações ativadas", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_users")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Notificações desativadas", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun loadSubscriptionData() {
        userId?.let { uid ->
            firestore.collection("subscriptions").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val isActive = document.getBoolean("isActive") == true
                        val endDate = document.getTimestamp("endDate")?.toDate()
                        val amount = document.getDouble("amount") ?: 25.0

                        binding.subscriptionStatus.text =
                            "Assinatura: ${if (isActive) "Ativa" else "Inativa"}"
                        binding.subscriptionEndDate.text = "Válida até: $endDate"
                        binding.subscriptionAmount.text = "Valor: R$ %.2f".format(amount)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao carregar assinatura",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun showDeleteDataConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Dados")
            .setMessage("Tem certeza de que deseja excluir todos os dados associados à sua conta? A assinatura será mantida.")
            .setPositiveButton("Excluir") { _, _ -> deleteUserData() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteUserData() {
        userId?.let { uid ->
            firestore.collection("bookings")
                .whereEqualTo("companyId", uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    for (document in snapshot) {
                        document.reference.delete()
                    }
                    deleteBusinessData(uid)
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Erro ao deletar agendamentos", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    private fun deleteBusinessData(uid: String) {
        firestore.collection("business").document(uid)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    "Dados excluídos com sucesso. Assinatura mantida.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Erro ao deletar dados do negócio", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}