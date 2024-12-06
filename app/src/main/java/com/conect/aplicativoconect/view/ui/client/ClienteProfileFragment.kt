package com.conect.aplicativoconect.view.ui.client

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentClienteProfileBinding
import com.conect.aplicativoconect.view.ui.PolicyActivity
import com.conect.aplicativoconect.view.ui.auth.LoginActivity
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class ClienteProfileFragment : Fragment() {

    private var _binding: FragmentClienteProfileBinding? = null
    private val binding get() = _binding!!
    private val clientViewModel: ClientViewModel by activityViewModels()
    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", false)
        binding.notificationsSwitch.isChecked = isNotificationsEnabled

        userId?.let {
            clientViewModel.loadUserData(it)
        }

        clientViewModel.userImage.observe(viewLifecycleOwner) { imageUrl ->
            Log.d("ClienteProfileFragment", "Loading image from URL: $imageUrl")
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.foto_perfil_generica)
                .error(R.drawable.foto_perfil_generica)
                .into(binding.profileImageClient)
        }

        clientViewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.userName.text = name
        }

        clientViewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.userEmail.text = email
        }

        binding.personalDataButton.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        binding.helpButton.setOnClickListener {
            val message = "Olá, preciso de ajuda com o aplicativo."
            val url = "https://wa.me/5535984478656?text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        binding.aboutPolicy.setOnClickListener {
            val intent = Intent(requireContext(), PolicyActivity::class.java)
            startActivity(intent)
        }

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("notifications_enabled", isChecked)
            editor.apply()

            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Notificações ativadas", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Notificações desativadas", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.deleteAccountButton.setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Excluir Conta")
                .setMessage("Tem certeza de que deseja excluir sua conta? Esta ação não pode ser desfeita.")
                .setPositiveButton("Excluir") { _, _ -> deleteUserAccountAndData() }
                .setNegativeButton("Cancelar", null)
                .create()

            dialog.show()

            // Ajuste da cor da fonte do botão "Excluir" (positivo)
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.roxo)) // Cor roxa para o texto do botão "Excluir"

            // Ajuste da cor da fonte do botão "Cancelar" (negativo)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.roxo)) // Cor laranja para o texto do botão "Cancelar"
        }

    }

    private fun deleteUserAccountAndData() {
        userId?.let { uid ->
            firestore.collection("bookings")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    for (document in snapshot) {
                        document.reference.delete()
                    }
                    deleteUserDocument(uid)
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Erro ao deletar agendamentos", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteUserDocument(uid: String) {
        firestore.collection("users").document(uid)
            .delete()
            .addOnSuccessListener {
                deleteUserAccount()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Erro ao deletar dados do usuário", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteUserAccount() {
        FirebaseAuth.getInstance().currentUser?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Conta e dados excluídos com sucesso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    activity?.finish()
                } else {
                    Toast.makeText(context, "Falha ao excluir a conta", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    activity?.finish()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
