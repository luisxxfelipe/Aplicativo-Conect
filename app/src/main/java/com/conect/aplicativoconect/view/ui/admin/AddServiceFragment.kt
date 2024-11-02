package com.conect.aplicativoconect.view.ui.admin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.ServiceType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddServiceFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addedServicesRecyclerView: RecyclerView
    private lateinit var serviceAdapter: ServiceTypeAdapter
    private lateinit var addedServicesAdapter: AddedServicesAdapter
    private lateinit var firestore: FirebaseFirestore
    private val addedServices = mutableListOf<Pair<ServiceType, Double>>()
    private lateinit var editTextPrice: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonAdd: Button

    private var availableServices = mutableListOf<ServiceType>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_service, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewServiceTypes)
        addedServicesRecyclerView = view.findViewById(R.id.recyclerViewAddedServices)
        editTextPrice = view.findViewById(R.id.editTextPrice)
        buttonSave = view.findViewById(R.id.buttonSaveServices)
        buttonAdd = view.findViewById(R.id.buttonAddService)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        addedServicesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 1)

        firestore = FirebaseFirestore.getInstance()
        fetchBusinessType()

        buttonAdd.setOnClickListener {
            hideKeyboard()
            addSelectedService()
        }
        buttonSave.setOnClickListener { saveServices() }

        return view
    }

    private fun fetchBusinessType() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("business").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val serviceType =
                        document.getString("serviceType") ?: return@addOnSuccessListener
                    val services =
                        document.get("services") as? List<HashMap<String, Any>> ?: emptyList()

                    services.forEach { service ->
                        val serviceName = service["serviceName"] as String
                        val price = (service["price"] as Number).toDouble()
                        addedServices.add(Pair(ServiceType(serviceName), price))
                    }

                    availableServices =
                        getAvailableServiceTypesForCategory(serviceType).toMutableList()
                    serviceAdapter = ServiceTypeAdapter(availableServices) { updateUI() }
                    recyclerView.adapter = serviceAdapter

                    updateAddedServicesRecyclerView()
                    updateUI()
                }
                .addOnFailureListener { /* Tratar erro */ }
        }
    }

    private fun getAvailableServiceTypesForCategory(serviceType: String): List<ServiceType> {
        val allServices = when (serviceType) {
            "Cabeleireiro" -> listOf(
                ServiceType("Corte de Cabelo"),
                ServiceType("Penteado"),
                ServiceType("Coloração"),
                ServiceType("Tratamento Capilar"),
                ServiceType("Alongamento de Cílios"),
                ServiceType("Escova Progressiva"),
                ServiceType("Botox Capilar"),
                ServiceType("Luzes e Mechas")
            )

            "Barbeiro" -> listOf(
                ServiceType("Corte de Cabelo"),
                ServiceType("Corte de Barba"),
                ServiceType("Aparar Barba"),
                ServiceType("Design de Barba"),
                ServiceType("Hidratação Facial"),
                ServiceType("Pigmentação de Barba"),
                ServiceType("Camuflagem de Fios Brancos")
            )

            "Manicure" -> listOf(
                ServiceType("Manicure Clássica"),
                ServiceType("Pedicure"),
                ServiceType("Aplicação de Unhas de Gel"),
                ServiceType("Nail Art"),
                ServiceType("Spa para Pés"),
                ServiceType("Fortalecimento de Unhas"),
                ServiceType("Unhas Acrílicas")
            )

            "Estética" -> listOf(
                ServiceType("Depilação"),
                ServiceType("Limpeza de Pele"),
                ServiceType("Maquiagem"),
                ServiceType("Design de Sobrancelhas"),
                ServiceType("Peeling Facial"),
                ServiceType("Tratamento Antienvelhecimento"),
                ServiceType("Bronzeamento Artificial")
            )

            "Massagem" -> listOf(
                ServiceType("Massagem Relaxante"),
                ServiceType("Massagem Terapêutica"),
                ServiceType("Drenagem Linfática"),
                ServiceType("Massagem com Pedras Quentes"),
                ServiceType("Shiatsu"),
                ServiceType("Reflexologia"),
                ServiceType("Massagem Ayurvédica")
            )

            else -> emptyList()
        }

        return allServices.filterNot { addedServices.map { it.first.name }.contains(it.name) }
    }

    private fun addSelectedService() {
        val selectedService = serviceAdapter.getSelectedService() // Obtém o serviço selecionado
        val priceInput = editTextPrice.text.toString().toDoubleOrNull()

        if (selectedService != null && priceInput != null) {
            // Adiciona o serviço à lista
            addedServices.add(Pair(selectedService, priceInput))
            availableServices.remove(selectedService) // Remove da lista disponível
            updateAddedServicesRecyclerView()
            serviceAdapter.notifyDataSetChanged()

            // Limpa o campo de preço e reseta a seleção do adaptador
            editTextPrice.text.clear()
            serviceAdapter.clearSelection() // Novo método para limpar a seleção
            updateUI()
        } else {
            // Mostra um Toast se faltou selecionar um serviço ou inserir um preço
            Toast.makeText(
                requireContext(),
                "Selecione um serviço e insira um preço válido.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun updateAddedServicesRecyclerView() {
        if (!::addedServicesAdapter.isInitialized) {
            addedServicesAdapter = AddedServicesAdapter(addedServices) { position ->
                removeService(position)
            }
            addedServicesRecyclerView.adapter = addedServicesAdapter
        } else {
            addedServicesAdapter.notifyDataSetChanged()
        }
    }

    private fun removeService(position: Int) {
        val serviceToRemove = addedServices[position]
        addedServices.removeAt(position)
        availableServices.add(serviceToRemove.first)
        updateAddedServicesRecyclerView()
        serviceAdapter.notifyDataSetChanged()
        updateUI()
    }

    private fun saveServices() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val serviceData = addedServices.map { service ->
                hashMapOf(
                    "serviceName" to service.first.name,
                    "price" to service.second
                )
            }

            firestore.collection("business").document(userId)
                .update("services", serviceData)
                .addOnSuccessListener {
                    // Exibe um Toast indicando sucesso
                    Toast.makeText(
                        requireContext(),
                        "Serviços salvos com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Redireciona para a tela principal do admin
                    navigateToAdminHome()
                }
                .addOnFailureListener { e ->
                    // Exibe um Toast indicando falha
                    Toast.makeText(
                        requireContext(),
                        "Erro ao salvar serviços: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun updateUI() {
        buttonSave.visibility = if (addedServices.isEmpty()) View.GONE else View.VISIBLE
        recyclerView.visibility = if (availableServices.isEmpty()) View.GONE else View.VISIBLE
        editTextPrice.visibility = if (availableServices.isEmpty()) View.GONE else View.VISIBLE
        buttonAdd.visibility = if (availableServices.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun navigateToAdminHome() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AdminHomeFragment())
            .commit()
    }


}