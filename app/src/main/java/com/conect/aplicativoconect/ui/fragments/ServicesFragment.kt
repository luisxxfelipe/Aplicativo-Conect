package com.conect.aplicativoconect.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Service
import com.conect.aplicativoconect.ui.adapters.ServicesAdapter
import com.conect.aplicativoconect.ui.viewmodels.CompanyViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ServicesFragment : Fragment() {

    private lateinit var companyId: String
    private lateinit var servicesRecyclerView: RecyclerView
    private lateinit var serviceTypeBadge: TextView
    private val companyViewModel: CompanyViewModel by activityViewModels()
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            companyId = it.getString("companyId") ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_services, container, false)

        // Inicializar Firestore
        firestore = FirebaseFirestore.getInstance()

        // Referências das views
        servicesRecyclerView = view.findViewById(R.id.servicesRecyclerView)
        serviceTypeBadge = view.findViewById(R.id.serviceTypeBadge)

        // Configurar RecyclerView
        servicesRecyclerView.layoutManager = GridLayoutManager(context, 1)

        // Configurar observers
        setupObservers()

        // Carregar dados da empresa e serviços
        if (companyId.isNotEmpty()) {
            loadCompanyInfo(companyId)
            companyViewModel.loadServices(companyId)
        }

        return view
    }

    private fun setupObservers() {
        companyViewModel.services.observe(viewLifecycleOwner) { services ->
            setupAdapter(services)
        }
        
        companyViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Implementar indicador de loading se necessário
        }
        
        companyViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, "Erro ao carregar serviços: $it", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupAdapter(serviceList: List<Service>) {
        val servicesAdapter = ServicesAdapter(requireContext(), serviceList, companyId)
        servicesRecyclerView.adapter = servicesAdapter
    }

    private fun loadCompanyInfo(companyId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val document = firestore.collection("business").document(companyId).get().await()
                withContext(Dispatchers.Main) {
                    if (document.exists()) {
                        val serviceType = document.getString("serviceType") ?: "Serviços Gerais"
                        serviceTypeBadge.text = serviceType
                    } else {
                        serviceTypeBadge.text = "Serviços Gerais"
                        Log.w("ServicesFragment", "Empresa não encontrada: $companyId")
                    }
                }
            } catch (e: Exception) {
                Log.e("ServicesFragment", "Erro ao carregar informações da empresa: ${e.message}")
                withContext(Dispatchers.Main) {
                    serviceTypeBadge.text = "Serviços Gerais"
                }
            }
        }
    }

    companion object {
        fun newInstance(companyId: String) = ServicesFragment().apply {
            arguments = Bundle().apply {
                putString("companyId", companyId)
            }
        }
    }
}
