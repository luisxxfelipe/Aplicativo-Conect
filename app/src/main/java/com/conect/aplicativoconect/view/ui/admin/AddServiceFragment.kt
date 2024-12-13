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
    private val addedServices = mutableListOf<Pair<ServiceType, Double>>()
    private lateinit var editTextPrice: EditText
    private lateinit var editTextServiceName: EditText
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
        buttonSave = view.findViewById(R.id.buttonSaveServices)
        buttonAdd = view.findViewById(R.id.buttonAddService)

        // Configurar RecyclerView
        addedServicesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        firestore = FirebaseFirestore.getInstance()

        // Configurar botões
        buttonAdd.setOnClickListener { addCustomService() }
        buttonSave.setOnClickListener { saveServices() }

        // Atualizar UI inicial
        updateAddedServicesRecyclerView()
        updateUI()

        return view
    }

    private fun addCustomService() {
        val serviceName = editTextServiceName.text.toString().trim()
        val price = editTextPrice.text.toString().toDoubleOrNull()

        if (serviceName.isEmpty()) {
            Toast.makeText(requireContext(), "Digite o nome do serviço.", Toast.LENGTH_SHORT).show()
            return
        }

        if (price == null) {
            Toast.makeText(requireContext(), "Digite um preço válido.", Toast.LENGTH_SHORT).show()
            return
        }

        // Adicionar o serviço à lista
        addedServices.add(Pair(ServiceType(serviceName), price))
        updateAddedServicesRecyclerView()

        // Limpar os campos
        editTextServiceName.text.clear()
        editTextPrice.text.clear()
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
