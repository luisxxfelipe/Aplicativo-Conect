package com.conect.aplicativoconect.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.ui.viewmodels.CompanyViewModel
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Service
import com.conect.aplicativoconect.ui.adapters.ServicesAdapter
import com.google.firebase.firestore.FirebaseFirestore

class ServicesFragment : Fragment() {

    private lateinit var companyId: String
    private lateinit var servicesRecyclerView: RecyclerView
    private val companyViewModel: CompanyViewModel by activityViewModels()

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
        servicesRecyclerView = view.findViewById(R.id.servicesRecyclerView)
        servicesRecyclerView.layoutManager = GridLayoutManager(context, 2)
        
        setupObservers()
        if (companyId.isNotEmpty()) {
            companyViewModel.loadServices(companyId)
        }
        
        return view
    }

    private fun setupObservers() {
        companyViewModel.services.observe(viewLifecycleOwner) { services ->
            setupAdapter(services)
        }
        
        companyViewModel.isLoading.observe(viewLifecycleOwner) { _ ->
            // ✅ Loading será implementado conforme necessidade futura
        }
        
        companyViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
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
