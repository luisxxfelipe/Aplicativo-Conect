package com.conect.aplicativoconect.view.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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

    private val addedServices = mutableListOf<Pair<ServiceType, Double>>() // Lista de serviços adicionados
    private lateinit var editTextPrice: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_service, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewServiceTypes)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2) // 2 colunas

        addedServicesRecyclerView = view.findViewById(R.id.recyclerViewAddedServices)
        addedServicesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 1) // 1 coluna

        editTextPrice = view.findViewById(R.id.editTextPrice)

        firestore = FirebaseFirestore.getInstance()
        fetchBusinessType()

        view.findViewById<Button>(R.id.buttonAddService).setOnClickListener {
            addSelectedService()
        }

        view.findViewById<Button>(R.id.buttonSaveServices).setOnClickListener {
            saveServices()
        }

        return view
    }

    private fun fetchBusinessType() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            firestore.collection("business").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val serviceType = document.getString("serviceType")
                        if (serviceType != null) {
                            serviceAdapter = ServiceTypeAdapter(getServiceTypesForCategory(serviceType)) { serviceType ->
                                // Atualiza a seleção do serviço
                            }
                            recyclerView.adapter = serviceAdapter
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Tratar erro ao buscar dados
                }
        }
    }

    private fun getServiceTypesForCategory(serviceType: String): List<ServiceType> {
        return when (serviceType) {
            "Cabeleireiro" -> listOf(
                ServiceType("Corte de Cabelo"),
                ServiceType("Penteado"),
                ServiceType("Coloração"),
                ServiceType("Tratamento Capilar"),
                ServiceType("Alongamento de Cílios")
            )
            "Barbeiro" -> listOf(
                ServiceType("Corte de Cabelo"),
                ServiceType("Corte de Barba"),
                ServiceType("Aparar Barba"),
                ServiceType("Design de Barba")
            )
            "Manicure" -> listOf(
                ServiceType("Manicure Clássica"),
                ServiceType("Pedicure"),
                ServiceType("Aplicação de Unhas de Gel"),
                ServiceType("Nail Art")
            )
            "Estética" -> listOf(
                ServiceType("Depilação"),
                ServiceType("Limpeza de Pele"),
                ServiceType("Maquiagem"),
                ServiceType("Design de Sobrancelhas")
            )
            "Massagem" -> listOf(
                ServiceType("Massagem Relaxante"),
                ServiceType("Massagem Terapêutica"),
                ServiceType("Drenagem Linfática"),
                ServiceType("Massagem com Pedras Quentes")
            )
            else -> emptyList() // Caso não encontre o tipo
        }
    }

    private fun addSelectedService() {
        val selectedService = serviceAdapter.getSelectedService() // Obtém o serviço selecionado
        val priceInput = editTextPrice.text.toString().toDoubleOrNull()

        if (selectedService != null && priceInput != null) {
            addedServices.add(Pair(selectedService, priceInput))
            updateAddedServicesRecyclerView()
            editTextPrice.text.clear()
        }
    }

    private fun updateAddedServicesRecyclerView() {
        if (!::addedServicesAdapter.isInitialized) {
            addedServicesAdapter = AddedServicesAdapter(addedServices) { service, price ->
                // Aqui você pode lidar com a edição do preço se necessário
            }
            addedServicesRecyclerView.adapter = addedServicesAdapter
        } else {
            addedServicesAdapter.notifyDataSetChanged()
        }
    }

    private fun saveServices() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Salve a lista de serviços no Firestore
            val serviceData = addedServices.map { service ->
                hashMapOf(
                    "serviceName" to service.first.name,
                    "price" to service.second
                )
            }

            firestore.collection("business").document(userId)
                .update("services", serviceData)
                .addOnSuccessListener {
                    // Serviços salvos com sucesso
                }
                .addOnFailureListener { e ->
                    // Tratar erro ao salvar
                }
        }
    }
}
