package com.conect.aplicativoconect.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Service
import com.conect.aplicativoconect.ui.adapters.ServicesAdapter
import com.google.firebase.firestore.FirebaseFirestore

class ServicesFragment : Fragment() {

    private lateinit var companyId: String
    private lateinit var firestore: FirebaseFirestore
    private lateinit var servicesRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            companyId = it.getString("companyId") ?: ""
        }
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_services, container, false)
        servicesRecyclerView = view.findViewById(R.id.servicesRecyclerView)
        servicesRecyclerView.layoutManager = GridLayoutManager(context, 2)
        fetchServices()
        return view
    }

    private fun fetchServices() {
        firestore.collection("business").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val services =
                        document.get("services") as? List<Map<String, Any>> ?: emptyList()
                    val serviceList = services.mapNotNull { serviceMap ->
                        val name = serviceMap["serviceName"] as? String
                        val price = (serviceMap["price"] as? Number)?.toDouble()
                        if (name != null && price != null) {
                            Service(name, price)
                        } else null
                    }
                    setupAdapter(serviceList)
                } else {
                    Toast.makeText(context, "Serviços não encontrados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Erro ao carregar serviços", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupAdapter(serviceList: List<Service>) {
        val servicesAdapter = ServicesAdapter(requireContext(), serviceList, companyId)
        servicesRecyclerView.adapter = servicesAdapter
    }

    companion object {
        fun newInstance(companyId: String) = ServicesFragment().apply {
            arguments = Bundle().apply {
                putString("companyId", companyId)
            }
        }
    }
}
