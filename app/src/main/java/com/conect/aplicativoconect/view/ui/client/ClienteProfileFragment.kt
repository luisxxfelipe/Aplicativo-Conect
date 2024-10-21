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

class ClienteProfileFragment : Fragment() {

    private lateinit var clientViewModel: ClientViewModel
    private lateinit var userNameTextView: TextView
    // Adicione mais TextViews conforme necessário para mostrar outros dados do usuário

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cliente_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userNameTextView = view.findViewById(R.id.userName)

        clientViewModel = ViewModelProvider(this).get(ClientViewModel::class.java)

        // Observe as informações do perfil
        clientViewModel.userData.observe(viewLifecycleOwner) { user ->
            userNameTextView.text = user.displayName // Ajuste conforme os dados disponíveis
            // Atualize outros TextViews conforme necessário
        }

        // Carregar dados do perfil - Use um ID de usuário apropriado
        clientViewModel.loadUserData("userId") // Altere "userId" para o ID correto
    }
}
