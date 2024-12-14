package com.conect.aplicativoconect.view.ui.admin

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.ServiceType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddServiceFragment : Fragment() {

    private lateinit var addedServicesRecyclerView: RecyclerView
    private lateinit var addedServicesAdapter: AddedServicesAdapter
    private lateinit var firestore: FirebaseFirestore
    private val addedServices = mutableListOf<Triple<ServiceType, Double, Int>>()
    private lateinit var editTextPrice: EditText
    private lateinit var editTextServiceName: EditText
    private lateinit var editTextDuration: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonAdd: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_service, container, false)

        // Inicializar os elementos de UI
        addedServicesRecyclerView = view.findViewById(R.id.recyclerViewAddedServices)
        editTextPrice = view.findViewById(R.id.editTextPrice)
        editTextServiceName = view.findViewById(R.id.editTextServiceName)
        editTextDuration = view.findViewById(R.id.editTextDuration) // Campo para duração
        buttonSave = view.findViewById(R.id.buttonSaveServices)
        buttonAdd = view.findViewById(R.id.buttonAddService)

        // Configurar RecyclerView
        addedServicesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        firestore = FirebaseFirestore.getInstance()

        // Configurar botões
        buttonAdd.setOnClickListener { addCustomService() }
        buttonSave.setOnClickListener { saveServices() }

        // Buscar serviços existentes
        fetchExistingServices()

        return view
    }


    private fun fetchExistingServices() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("business").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val services = document.get("services") as? List<Map<String, Any>> ?: emptyList()

                // Mapeia os serviços do Firestore para a lista `addedServices`
                addedServices.clear()
                addedServices.addAll(
                    services.map {
                        Triple(
                            ServiceType(it["serviceName"] as String),
                            (it["price"] as Number).toDouble(),
                            (it["duration"] as Number).toInt()
                        )
                    }
                )

                // Atualiza o RecyclerView
                updateAddedServicesRecyclerView()
                updateUI()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Erro ao buscar serviços: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun addCustomService() {
        val serviceName = editTextServiceName.text.toString().trim()
        val price = editTextPrice.text.toString().toDoubleOrNull()
        val duration = editTextDuration.text.toString().toIntOrNull()

        if (serviceName.isEmpty()) {
            Toast.makeText(requireContext(), "Digite o nome do serviço.", Toast.LENGTH_SHORT).show()
            return
        }

        if (price == null) {
            Toast.makeText(requireContext(), "Digite um preço válido.", Toast.LENGTH_SHORT).show()
            return
        }

        if (duration == null || duration <= 0) {
            Toast.makeText(
                requireContext(),
                "Digite uma duração válida em minutos.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Adicionar o serviço à lista
        addedServices.add(Triple(ServiceType(serviceName), price, duration))
        updateAddedServicesRecyclerView()

        // Recalcular a média e salvar no Firestore
        updateAverageDuration()

        // Limpar os campos
        editTextServiceName.text.clear()
        editTextPrice.text.clear()
        editTextDuration.text.clear()
        hideKeyboard()
        updateUI()
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
        addedServices.removeAt(position)
        updateAddedServicesRecyclerView()

        // Recalcular a média e salvar no Firestore
        updateAverageDuration()

        updateUI()
    }

    private fun updateAverageDuration() {
        if (addedServices.isEmpty()) {
            saveAverageDuration(0.0)
            return
        }

        // Calcular a média de duração dos serviços
        val totalDuration = addedServices.sumOf { it.third }
        val averageDuration = totalDuration.toDouble() / addedServices.size

        // Salvar a média no Firestore
        saveAverageDuration(averageDuration)
    }

    private fun saveAverageDuration(averageDuration: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val businessRef = firestore.collection("business").document(userId)

        // Atualiza ou cria o campo `averageDuration` no documento
        businessRef.update("averageDuration", averageDuration)
            .addOnSuccessListener {
                Log.d("AddServiceFragment", "Média de duração atualizada: $averageDuration")
            }
            .addOnFailureListener { e ->
                Log.e("AddServiceFragment", "Erro ao atualizar a média de duração: ${e.message}")

                // Caso o documento não exista, cria-o com o campo `averageDuration`
                businessRef.set(mapOf("averageDuration" to averageDuration))
                    .addOnSuccessListener {
                        Log.d(
                            "AddServiceFragment",
                            "Documento criado com média de duração: $averageDuration"
                        )
                    }
                    .addOnFailureListener { error ->
                        Log.e("AddServiceFragment", "Erro ao criar documento: ${error.message}")
                    }
            }
    }


    private fun saveServices() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val serviceData = addedServices.map { service ->
                hashMapOf(
                    "serviceName" to service.first.name,
                    "price" to service.second,
                    "duration" to service.third // Salva a duração do serviço
                )
            }

            firestore.collection("business").document(userId)
                .update("services", serviceData)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Serviços salvos com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToAdminHome()
                }
                .addOnFailureListener { e ->
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
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        view?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun navigateToAdminHome() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AdminHomeFragment())
            .commit()
    }
}
