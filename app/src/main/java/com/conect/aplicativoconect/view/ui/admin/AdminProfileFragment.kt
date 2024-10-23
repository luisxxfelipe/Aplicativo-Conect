package com.conect.aplicativoconect.view.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentAdminProfileBinding
import com.conect.aplicativoconect.view.viewmodel.AdminViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage

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

        // Inicializando o ViewModel
        adminViewModel = ViewModelProvider(this).get(AdminViewModel::class.java)
        firestore = FirebaseFirestore.getInstance()

        // Observar o nome do administrador
        adminViewModel.adminName.observe(viewLifecycleOwner) { name ->
            binding.userNameAdmin.text = name ?: "Nome não disponível"
        }

        // Observar o e-mail do administrador
        adminViewModel.adminEmail.observe(viewLifecycleOwner) { email ->
            binding.userEmailAdmin.text = email ?: "Email não disponível"
        }

        // Carregar a imagem de perfil do administrador do Firestore
        loadProfileImage()

        // Carregar os dados do administrador do Firestore
        adminViewModel.loadAdminData()

        // Listener para abrir o fragmento de horários de funcionamento
        binding.hoursButtonAdmin.setOnClickListener {
            openOperatingHoursFragment()
        }
    }

    private fun loadProfileImage() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Obtendo o ID do usuário

        // Recuperar a URL da imagem de perfil do Firestore
        firestore.collection("business")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val imageUrl = document.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        // Usar Glide para carregar a imagem de perfil na CircleImageView
                        Glide.with(this)
                            .load(imageUrl) // Carregar a URL diretamente
                            .placeholder(R.drawable.foto_perfil_generica) // Imagem de placeholder enquanto carrega
                            .error(R.drawable.foto_perfil_generica) // Imagem de erro, caso falhe
                            .into(binding.profileImageAdmin)
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Lidar com falha ao obter a imagem
                Log.e("AdminProfileFragment", "Error loading profile image", exception)
            }
    }



    private fun openOperatingHoursFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, OperatingHoursFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
