package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla o layout usando ViewBinding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Simulação da ausência de dados para os RecyclerViews
        displayEmptyMessages()
    }

    private fun displayEmptyMessages() {
        // Aqui você pode definir a lógica para verificar se há dados
        // Simulando que não há dados
        val hasBookings = false
        val hasCategories = false
        val hasEstablishments = false

        // Mostrar/ocultar mensagens e RecyclerViews com base nos dados
        if (hasBookings) {
            binding.todayBookingsRecyclerView.visibility = View.VISIBLE
            binding.noBookingsMessage.visibility = View.GONE
        } else {
            binding.todayBookingsRecyclerView.visibility = View.GONE
            binding.noBookingsMessage.visibility = View.VISIBLE
        }

        if (hasCategories) {
            binding.categoriesRecyclerView.visibility = View.VISIBLE
            binding.noCategoriesMessage.visibility = View.GONE
        } else {
            binding.categoriesRecyclerView.visibility = View.GONE
            binding.noCategoriesMessage.visibility = View.VISIBLE
        }

        if (hasEstablishments) {
            binding.establishmentsRecyclerView.visibility = View.VISIBLE
            binding.noEstablishmentsMessage.visibility = View.GONE
        } else {
            binding.establishmentsRecyclerView.visibility = View.GONE
            binding.noEstablishmentsMessage.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpe a referência para evitar leaks
    }
}