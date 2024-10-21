package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.firebase.auth.FirebaseAuth

class ClienteProfileFragment : Fragment() {

    private lateinit var clientViewModel: ClientViewModel
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cliente_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userNameTextView = view.findViewById(R.id.userName)
        userEmailTextView = view.findViewById(R.id.userEmail)

        clientViewModel = ViewModelProvider(this)[ClientViewModel::class.java]

        // Obtenha o userId do Firebase Authentication
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        userId?.let {
            // Carregar dados do perfil
            clientViewModel.loadUserData(it)
        } ?: run {
            // Trate o caso quando não há usuário autenticado
            userNameTextView.text = "Usuário não autenticado"
            userEmailTextView.text = "Usuário não autenticado"
        }

        // Observe as informações do perfil
        clientViewModel.userData.observe(viewLifecycleOwner) { user ->
            userNameTextView.text = user?.name ?: "Nome não disponível" // Altere displayName para name
            userEmailTextView.text = user?.email ?: "Email não disponível"
        }
    }
}
