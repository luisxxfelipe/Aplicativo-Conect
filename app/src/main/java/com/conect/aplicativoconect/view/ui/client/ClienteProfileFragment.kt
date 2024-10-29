package com.conect.aplicativoconect.view.ui.client

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentClienteProfileBinding
import com.conect.aplicativoconect.view.ui.PolicyActivity
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.firebase.auth.FirebaseAuth

class ClienteProfileFragment : Fragment() {

    private var _binding: FragmentClienteProfileBinding? = null
    private val binding get() = _binding!!
    private val clientViewModel: ClientViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtenha o ID do usuário autenticado
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            clientViewModel.loadUserData(it) // Carregue os dados do usuário
        }

        // Observe a URL da imagem de perfil
        clientViewModel.userImage.observe(viewLifecycleOwner) { imageUrl ->
            Log.d("ClienteProfileFragment", "Loading image from URL: $imageUrl")
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.foto_perfil_generica) // Imagem padrão enquanto carrega
                .error(R.drawable.foto_perfil_generica) // Imagem de erro, se houver falha no carregamento
                .into(binding.profileImageClient) // binding.userImage é o seu ImageView
        }

        // Observe o nome do usuário
        clientViewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.userName.text = name // Atualiza o TextView com o nome
        }

        // Observe o email do usuário
        clientViewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.userEmail.text = email // Atualiza o TextView com o email
        }

        binding.personalDataButton.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Redireciona para o WhatsApp
        binding.helpButton.setOnClickListener {
            val message = "Olá, preciso de ajuda com o aplicativo."
            val url = "https://wa.me/5535984478656?text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // Redireciona para a tela de política de privacidade
        binding.aboutPolicy.setOnClickListener {
            val intent = Intent(requireContext(), PolicyActivity::class.java)
            startActivity(intent)
        }

    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
