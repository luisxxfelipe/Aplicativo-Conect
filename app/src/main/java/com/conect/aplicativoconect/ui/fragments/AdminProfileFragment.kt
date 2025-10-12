package com.conect.aplicativoconect.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentAdminProfileBinding
import com.conect.aplicativoconect.ui.activities.EditBusinessProfileActivity
import com.conect.aplicativoconect.ui.activities.PolicyActivity
import com.conect.aplicativoconect.ui.viewmodels.AdminViewModel
import com.conect.aplicativoconect.utils.AuthHelper
import com.conect.aplicativoconect.utils.ImageHelper
import com.google.firebase.firestore.FirebaseFirestore

class AdminProfileFragment : Fragment() {

    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var adminViewModel: AdminViewModel
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa o ViewModel
        adminViewModel = ViewModelProvider(this).get(AdminViewModel::class.java)
        firestore = FirebaseFirestore.getInstance()

        // Observar nome e email do administrador
        adminViewModel.adminName.observe(viewLifecycleOwner) { name ->
            binding.userNameAdmin.text = name ?: "Nome não disponível"
        }

        adminViewModel.adminEmail.observe(viewLifecycleOwner) { email ->
            binding.userEmailAdmin.text = email ?: "Email não disponível"
        }

        // Observar endereço
        adminViewModel.address.observe(viewLifecycleOwner) { address ->
            binding.address.text = address ?: "Endereço não disponível"
        }

        // Observar horário de funcionamento
        adminViewModel.operatingHours.observe(viewLifecycleOwner) { hours ->
            binding.horarioFuncionamento.text = hours ?: "Horário não disponível"
        }

        // Carregar imagem de perfil
        loadProfileImage()

        // Carregar dados do administrador do Firestore
        adminViewModel.loadAdminData()

        // Configuração dos botões
        binding.dadospessoais.setOnClickListener {
            val intent = Intent(requireContext(), EditBusinessProfileActivity::class.java)
            startActivity(intent)
        }

        binding.helpButtonAdmin.setOnClickListener {
            val message = "Olá, preciso de ajuda com o aplicativo."
            val url = "https://wa.me/5535984478656?text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        binding.aboutpolicy.setOnClickListener {
            val intent = Intent(requireContext(), PolicyActivity::class.java)
            startActivity(intent)
        }

        binding.settingsButtonAdmin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.adminFragmentContainer, AdminSettingsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadProfileImage() {
        val userId = AuthHelper.getCurrentUserId() ?: return // ✅ OTIMIZADO: Helper centralizado
        firestore.collection("business")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val imageUrl = document.getString("imageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    ImageHelper.loadProfileImage(requireContext(), imageUrl, binding.profileImageAdmin) // ✅ OTIMIZADO: Helper centralizado
                }
            }
            .addOnFailureListener {
                // Tratar falha ao carregar imagem
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
