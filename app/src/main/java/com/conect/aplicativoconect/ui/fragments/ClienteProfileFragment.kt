package com.conect.aplicativoconect.ui.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentClienteProfileBinding
import com.conect.aplicativoconect.ui.activities.EditProfileActivity
import com.conect.aplicativoconect.ui.activities.PolicyActivity
import com.conect.aplicativoconect.ui.viewmodels.ClientViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class ClienteProfileFragment : Fragment() {

    private var _binding: FragmentClienteProfileBinding? = null
    private val binding get() = _binding!!
    private val clientViewModel: ClientViewModel by activityViewModels()
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

        // Carregar as configurações de notificações
        val sharedPreferences =
            requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", false)
        binding.notificationsSwitch.isChecked = isNotificationsEnabled

        // Carregar os dados do usuário
        userId?.let {
            clientViewModel.loadUserData(it)
        }

        // Observar a imagem do usuário
        clientViewModel.userImage.observe(viewLifecycleOwner) { imageUrl ->
            Log.d("ClienteProfileFragment", "Carregando imagem da URL: $imageUrl")
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.foto_perfil_generica)  // Imagem placeholder enquanto carrega
                .error(R.drawable.foto_perfil_generica)  // Caso ocorra erro, imagem genérica
                .into(binding.profileImageClient)
        }

        // Observar o nome do usuário
        clientViewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.userName.text = name
        }

        // Observar o e-mail do usuário
        clientViewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.userEmail.text = email
        }

        // Ação do botão de editar dados pessoais
        binding.personalDataButton.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Ação do botão de ajuda via WhatsApp
        binding.helpButton.setOnClickListener {
            val message = "Olá, preciso de ajuda com o aplicativo."
            val url = "https://wa.me/5535984478656?text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // Ação do botão sobre as políticas
        binding.aboutPolicy.setOnClickListener {
            val intent = Intent(requireContext(), PolicyActivity::class.java)
            startActivity(intent)
        }

        // Ação do switch de notificações
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("notifications_enabled", isChecked)
            editor.apply()

            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Notificações ativadas", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Notificações desativadas", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
