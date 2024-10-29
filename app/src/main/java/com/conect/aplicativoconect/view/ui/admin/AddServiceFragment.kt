package com.conect.aplicativoconect.view.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
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
    private val addedServicesFromFirebase = mutableListOf<String>()
    private val addedServices = mutableListOf<Pair<ServiceType, Double>>() // Lista de serviços adicionados
    private lateinit var editTextPrice: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_service, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewServiceTypes)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1) // 1 coluna

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
                            // Recuperar os serviços já adicionados do Firebase
                            val services = document.get("services") as? List<HashMap<String, Any>> ?: emptyList()
                            services.forEach { service ->
                                val serviceName = service["serviceName"] as String
                                val price = (service["price"] as Number).toDouble() // Certifique-se de que o preço é um número
                                addedServices.add(Pair(ServiceType(serviceName), price))
                                addedServicesFromFirebase.add(serviceName) // Adicione também o nome para o filtro
                            }

                            // Filtrar serviços que ainda não foram adicionados
                            serviceAdapter = ServiceTypeAdapter(getAvailableServiceTypesForCategory(serviceType)) { serviceType ->
                                // Atualiza a seleção do serviço
                            }
                            recyclerView.adapter = serviceAdapter

                            // Atualize a RecyclerView com os serviços já adicionados
                            updateAddedServicesRecyclerView()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Tratar erro ao buscar dados
                }
        }
    }


    private fun getAvailableServiceTypesForCategory(serviceType: String): List<ServiceType> {
        val allServices = when (serviceType) {
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
            else -> emptyList()
        }

        // Filtra os serviços que já foram adicionados
        return allServices.filter { serviceType ->
            !addedServicesFromFirebase.contains(serviceType.name)
        }
    }

    private fun addSelectedService() {
        val selectedService = serviceAdapter.getSelectedService() // Obtém o serviço selecionado
        val priceInput = editTextPrice.text.toString().toDoubleOrNull()

        if (selectedService != null && priceInput != null) {
            addedServices.add(Pair(selectedService, priceInput))
            addedServicesFromFirebase.add(selectedService.name) // Adiciona o serviço à lista de filtragem
            updateAddedServicesRecyclerView()
            editTextPrice.text.clear()
        } else {
            // Tratar caso nenhum serviço tenha sido selecionado ou preço inválido
            Toast.makeText(requireContext(), "Selecione um serviço e insira um preço válido.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun updateAddedServicesRecyclerView() {
        if (!::addedServicesAdapter.isInitialized) {
            addedServicesAdapter = AddedServicesAdapter(addedServices) { position ->
                removeService(position) // Chama a função para remover serviço
            }
            addedServicesRecyclerView.adapter = addedServicesAdapter
        } else {
            addedServicesAdapter.notifyDataSetChanged()
        }
    }

    private fun removeService(position: Int) {
        // Remove o serviço da lista local
        val serviceToRemove = addedServices[position]
        addedServices.removeAt(position)
        addedServicesAdapter.notifyItemRemoved(position)

        // Atualiza o Firestore
        updateFirestoreServices()
    }

    private fun updateFirestoreServices() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Salve a lista atualizada de serviços no Firestore
            val serviceData = addedServices.map { service ->
                hashMapOf(
                    "serviceName" to service.first.name,
                    "price" to service.second
                )
            }

            firestore.collection("business").document(userId)
                .update("services", serviceData)
                .addOnSuccessListener {
                    // Atualização bem-sucedida
                }
                .addOnFailureListener { e ->
                    // Tratar erro ao atualizar
                }
        }
    }


    private fun saveServices() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Primeiro, obtenha os serviços existentes do Firestore
            firestore.collection("business").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        // Obtenha os serviços já existentes
                        val existingServices = document.get("services") as? List<HashMap<String, Any>> ?: emptyList()

                        // Construa a nova lista de serviços com os serviços existentes e os novos
                        val allServices = existingServices.toMutableList()

                        // Adicione os novos serviços, verificando se já existem
                        addedServices.forEach { service ->
                            val serviceName = service.first.name
                            val price = service.second

                            // Verifique se o serviço já existe antes de adicionar
                            if (allServices.none { it["serviceName"] == serviceName }) {
                                allServices.add(hashMapOf(
                                    "serviceName" to serviceName,
                                    "price" to price
                                ))
                            }
                        }

                        // Atualize a lista de serviços no Firestore
                        firestore.collection("business").document(userId)
                            .update("services", allServices)
                            .addOnSuccessListener {
                                // Serviços salvos com sucesso, redirecionar para a tela Home do Admin
                                navigateToAdminHome()
                            }
                            .addOnFailureListener { e ->
                                // Tratar erro ao salvar
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // Tratar erro ao buscar serviços existentes
                }
        }
    }


    private fun navigateToAdminHome() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AdminHomeFragment())
            .commit()
    }



}
