package com.conect.aplicativoconect.view.ui.client

import CategoriesPagerAdapter
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentClienteHomeBinding
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.ui.admin.BusinessAdapter
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ClienteHomeFragment : Fragment() {

    private var _binding: FragmentClienteHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var businessAdapter: BusinessAdapter
    private val businessList = mutableListOf<Business>()
    private val clientViewModel: ClientViewModel by activityViewModels()
    private val filteredBusinessList = mutableListOf<Business>()

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

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { clientViewModel.loadUserData(it) }

        clientViewModel.userName.observe(viewLifecycleOwner) { userName ->
            binding.userName.text = userName ?: "Nome do Usuário"
            updateGreeting()
        }

        clientViewModel.userImage.observe(viewLifecycleOwner) { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.foto_perfil_generica)
                .error(R.drawable.foto_perfil_generica)
                .into(binding.userImage)
        }

        setupAdapters()
        fetchBusinesses()
        setupSearchView()
        userId?.let { fetchUserBookings(it) }
    }

    private fun setupAdapters() {
        val categories = listOf("Manicure", "Barbearia", "Cabeleireiro", "Massagista", "Maquiagens")
        binding.categoriesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.categoriesRecyclerView.adapter = CategoriesPagerAdapter(categories) { selectedCategory ->
            filterBusinessesByCategory(selectedCategory)
        }

        businessAdapter = BusinessAdapter(requireContext(), businessList) { business ->
            fetchBusinessIdAndOpenDetails(business.name)
        }

        binding.establishmentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.establishmentsRecyclerView.adapter = businessAdapter
    }

    private fun filterBusinessesByCategory(category: String) {
        val filteredBusinesses = businessList.filter { it.serviceType == category }

        if (filteredBusinesses.isEmpty()) {
            Toast.makeText(requireContext(), "Nenhum estabelecimento encontrado.", Toast.LENGTH_SHORT).show()
        }

        businessAdapter = BusinessAdapter(requireContext(), filteredBusinesses) { business ->
            fetchBusinessIdAndOpenDetails(business.name)
        }
        binding.establishmentsRecyclerView.adapter = businessAdapter
        businessAdapter.notifyDataSetChanged()
    }

    private fun fetchBusinesses() {
        firestore.collection("business")
            .get()
            .addOnSuccessListener { querySnapshot ->
                businessList.clear()
                querySnapshot.documents.mapNotNullTo(businessList) {
                    it.toObject(Business::class.java)
                }
                filteredBusinessList.clear()
                filteredBusinessList.addAll(businessList)
                businessAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao buscar empresas.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 6 -> "Boa madrugada!"
            hour < 12 -> "Bom dia!"
            hour < 18 -> "Boa tarde!"
            else -> "Boa noite!"
        }
        binding.greetingTextView.text = greeting
    }


    private fun fetchBusinessIdAndOpenDetails(businessName: String) {
        firestore.collection("business")
            .whereEqualTo("name", businessName)
            .get()
            .addOnSuccessListener { documents ->
                documents.firstOrNull()?.id?.let { businessId ->
                    val intent = Intent(requireContext(), EmpresaDetalhesActivity::class.java).apply {
                        putExtra("companyId", businessId)
                    }
                    startActivity(intent)
                }
            }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterBusinesses(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterBusinesses(newText)
                return true
            }
        })
    }

    private fun filterBusinesses(query: String?) {
        filteredBusinessList.clear()

        if (query.isNullOrEmpty()) {
            filteredBusinessList.addAll(businessList)
            showAllSections(true)
        } else {
            val searchQuery = query.lowercase()
            filteredBusinessList.addAll(businessList.filter { it.name.lowercase().contains(searchQuery) })
            showAllSections(false)
        }

        businessAdapter = BusinessAdapter(requireContext(), filteredBusinessList) { business ->
            fetchBusinessIdAndOpenDetails(business.name)
        }
        binding.establishmentsRecyclerView.adapter = businessAdapter
        businessAdapter.notifyDataSetChanged()
    }

    private fun showAllSections(show: Boolean) {
        binding.todayAgendaCardView.visibility = if (show) View.VISIBLE else View.GONE
        binding.categoriesTitle.visibility = if (show) View.VISIBLE else View.GONE
        binding.categoriesRecyclerView.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun fetchUserBookings(userId: String) {
        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (_binding == null) return@addOnSuccessListener

                val bookings = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Booking::class.java)?.apply { id = document.id }
                }.filter { isFutureBooking(it) }
                    .sortedBy { it.date }
                    .take(2)

                if (bookings.isEmpty()) {
                    showNoBookingsMessage(true)
                } else {
                    showNoBookingsMessage(false)
                    binding.todayBookingsRecyclerView.layoutManager =
                        LinearLayoutManager(requireContext())
                    binding.todayBookingsRecyclerView.adapter = ClientBookingAdapter(
                        requireContext(), bookings, ::confirmBooking, ::cancelBooking
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClienteHomeFragment", "Erro ao buscar agendamentos: ${e.message}")
                Toast.makeText(requireContext(), "Erro ao buscar agendamentos.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showNoBookingsMessage(show: Boolean) {
        binding.noBookingsMessage.visibility = if (show) View.VISIBLE else View.GONE
        binding.noBookingsImage.visibility = if (show) View.VISIBLE else View.GONE
        binding.todayBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun confirmBooking(booking: Booking) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Agendamento")
            .setMessage("Tem certeza que deseja confirmar este agendamento?")
            .setPositiveButton("Sim") { dialog, _ ->
                firestore.collection("bookings").document(booking.id!!)
                    .update("status_cliente", "confirmed")
                    .addOnSuccessListener {
                        fetchUserBookings(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun cancelBooking(booking: Booking) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancelar Agendamento")
            .setMessage("Tem certeza que deseja cancelar este agendamento?")
            .setPositiveButton("Sim") { dialog, _ ->
                firestore.collection("bookings").document(booking.id!!)
                    .delete()
                    .addOnSuccessListener {
                        fetchUserBookings(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDateTime = Calendar.getInstance()
        val dateParts = booking.date?.split("/")?.map { it.toInt() } ?: return false

        val bookingDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, dateParts[2])
            set(Calendar.MONTH, dateParts[1] - 1)
            set(Calendar.DAY_OF_MONTH, dateParts[0])
            set(Calendar.HOUR_OF_DAY, booking.hour ?: 0)
        }
        return bookingDate.after(currentDateTime)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
