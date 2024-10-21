package com.conect.aplicativoconect.view.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentAdminProfileBinding
import com.conect.aplicativoconect.view.viewmodel.AdminViewModel

class AdminProfileFragment : Fragment() {

    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var adminViewModel: AdminViewModel

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

        // Observar o nome do administrador
        adminViewModel.adminName.observe(viewLifecycleOwner) { name ->
            binding.userNameAdmin.text = name ?: "Nome não disponível"
        }

        // Observar o e-mail do administrador
        adminViewModel.adminEmail.observe(viewLifecycleOwner) { email ->
            binding.userEmailAdmin.text = email ?: "Email não disponível"
        }

        // Carregar os dados do administrador do Firestore
        adminViewModel.loadAdminData()

        // Listener para abrir o fragmento de horários de funcionamento
        binding.hoursButtonAdmin.setOnClickListener {
            openOperatingHoursFragment()
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
