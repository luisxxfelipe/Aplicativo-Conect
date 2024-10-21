package com.conect.aplicativoconect.view.ui.client

import CategoriesPagerAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.conect.aplicativoconect.databinding.FragmentClienteHomeBinding
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.ui.BusinessAdapter
import com.conect.aplicativoconect.view.ui.admin.BookingAdapter
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.firebase.firestore.FirebaseFirestore

class ClienteHomeFragment : Fragment() {

    private var _binding: FragmentClienteHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var businessAdapter: BusinessAdapter
    private val businessList = mutableListOf<Business>()

    private val clientViewModel: ClientViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        clientViewModel.userName.observe(viewLifecycleOwner) { userName ->
            binding.userName.text = userName ?: "Nome do Usuário"
        }

        val userName = arguments?.getString("userName")
        if (userName != null) {
            clientViewModel.setUserName(userName)
        }

        val categories = listOf("Manicure", "Barbearia", "Cabeleireiro", "Massagista", "Maquiagens")
        val categoriesPagerAdapter = CategoriesPagerAdapter(categories)

        binding.categoriesRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesRecyclerView.adapter = categoriesPagerAdapter

        binding.establishmentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        businessAdapter = BusinessAdapter(businessList)
        binding.establishmentsRecyclerView.adapter = businessAdapter

        fetchBusinesses()

        // Observar as mudanças nas reservas de hoje
        clientViewModel.todayBookings.observe(viewLifecycleOwner) { bookings ->
            if (bookings.isNullOrEmpty()) {
                binding.noBookingsMessage.visibility = View.VISIBLE
                binding.noBookingsImage.visibility = View.VISIBLE
                binding.todayBookingsRecyclerView.visibility = View.GONE
            } else {
                binding.noBookingsMessage.visibility = View.GONE
                binding.noBookingsImage.visibility = View.GONE
                // Atualize o RecyclerView com os bookings
                binding.todayBookingsRecyclerView.adapter = BookingAdapter(bookings) // Crie um Adapter para bookings
                binding.todayBookingsRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun fetchBusinesses() {
        firestore.collection("business")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (_binding == null) return@addOnSuccessListener

                Log.d("HomeFragment", "Empresas encontradas: ${querySnapshot.size()}")
                if (!querySnapshot.isEmpty) {
                    businessList.clear()
                    for (document in querySnapshot.documents) {
                        val business = document.toObject(Business::class.java)
                        business?.let { businessList.add(it) }
                    }
                    businessAdapter.notifyDataSetChanged()
                    binding.noEstablishmentsMessage.visibility = View.GONE
                    binding.establishmentsRecyclerView.visibility = View.VISIBLE
                } else {
                    Log.d("HomeFragment", "Nenhuma empresa encontrada.")
                    binding.noEstablishmentsMessage.visibility = View.VISIBLE
                    binding.establishmentsRecyclerView.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener

                Log.e("HomeFragment", "Erro ao buscar empresas", e)
                Toast.makeText(requireContext(), "Erro ao buscar empresas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

