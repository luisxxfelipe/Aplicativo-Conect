package com.conect.aplicativoconect.view.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Service
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
    private val addedServices = mutableListOf<Service>()
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
                                val price = (service["price"] as Number).toDouble()
                                addedServices.add(Service(serviceName, price))
                                addedServicesFromFirebase.add(serviceName)
                            }

                            // Filtrar serviços que ainda não foram adicionados
                            serviceAdapter = ServiceTypeAdapter(getAvailableServiceTypesForCategory(serviceType)) { serviceType ->
                                // Atualiza a seleção do serviço
                            }
                            recyclerView.adapter = serviceAdapter

                            updateAvailableServices()
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

        // Filtra os serviços que já foram adicionados
        return allServices.filter { serviceType ->
            !addedServicesFromFirebase.contains(serviceType.name)
        }
    }

    private fun addSelectedService() {
        val selectedService = serviceAdapter.getSelectedService()
        val priceInput = editTextPrice.text.toString().toDoubleOrNull()

        if (selectedService != null && priceInput != null) {
            val service = Service(selectedService.name, priceInput)
            addedServices.add(service)
            addedServicesFromFirebase.add(service.name)
            updateAvailableServices()
            updateAddedServicesRecyclerView()
            editTextPrice.text.clear()
            serviceAdapter.clearSelection()
        } else {
            Toast.makeText(requireContext(), "Selecione um serviço e insira um preço válido.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("addedServices", ArrayList(addedServices))
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val restoredServices = savedInstanceState?.getSerializable("addedServices") as? ArrayList<Service>
        if (restoredServices != null) {
            addedServices.clear()
            addedServices.addAll(restoredServices)
            updateAddedServicesRecyclerView()
        }
    }


    private fun updateAvailableServices() {
        val remainingServices = serviceAdapter.serviceTypes.filter {
            !addedServicesFromFirebase.contains(it.name)
        }

        if (remainingServices.isEmpty()) {
            // Esconde os componentes que não são mais necessários
            view?.findViewById<TextView>(R.id.textViewPrompt)?.visibility = View.GONE
            view?.findViewById<RecyclerView>(R.id.recyclerViewServiceTypes)?.visibility = View.GONE
            view?.findViewById<EditText>(R.id.editTextPrice)?.visibility = View.GONE
            view?.findViewById<Button>(R.id.buttonAddService)?.visibility = View.GONE
        } else {
            // Atualiza o adapter e mantém a lista visível se ainda houver serviços
            serviceAdapter = ServiceTypeAdapter(remainingServices) { serviceType ->
                // Atualiza a seleção do serviço
            }
            recyclerView.adapter = serviceAdapter
            recyclerView.visibility = View.VISIBLE // Garante que o RecyclerView está visível
        }
    }

    private fun updateAddedServicesRecyclerView() {
        val buttonSave = view?.findViewById<Button>(R.id.buttonSaveServices)
        buttonSave?.visibility = if (addedServices.isEmpty()) View.GONE else View.VISIBLE

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
        addedServicesFromFirebase.remove(serviceToRemove.name)
        updateAvailableServices()
        updateAddedServicesRecyclerView()
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
                            val serviceName = service.name
                            val price = service.price

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
