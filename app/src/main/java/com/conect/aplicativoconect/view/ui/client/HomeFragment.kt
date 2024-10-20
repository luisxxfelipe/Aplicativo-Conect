package com.conect.aplicativoconect.view.ui.client

import CategoriesPagerAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.conect.aplicativoconect.databinding.FragmentHomeClienteBinding
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.ui.BusinessAdapter
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeClienteBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var businessAdapter: BusinessAdapter
    private val businessList = mutableListOf<Business>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeClienteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialize o Firestore
        firestore = FirebaseFirestore.getInstance()

        // Recuperar o nome do usuário dos argumentos
        val userName = arguments?.getString("userName")
        binding.userName.text = userName ?: "Nome do Usuário"

        // Configurar categorias
        val categories = listOf("Manicure", "Barbearia", "Cabeleireiro") // Substitua por suas categorias
        val categoriesPagerAdapter = CategoriesPagerAdapter(categories)
        binding.categoriesRecyclerView.adapter = categoriesPagerAdapter

        // Configuração do RecyclerView para estabelecimentos
        businessAdapter = BusinessAdapter(businessList)
        binding.establishmentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = businessAdapter
        }

        // Buscar empresas
        fetchBusinesses()
    }



    private fun fetchBusinesses() {
        firestore.collection("business")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("HomeFragment", "Empresas encontradas: ${querySnapshot.size()}")
                if (!querySnapshot.isEmpty) {
                    businessList.clear() // Limpe a lista anterior
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
                Log.e("HomeFragment", "Erro ao buscar empresas", e)
                Toast.makeText(
                    requireContext(),
                    "Erro ao buscar empresas: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

        override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
