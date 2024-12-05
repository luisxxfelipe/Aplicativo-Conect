package com.conect.aplicativoconect.view.ui.client

import CategoriesPagerAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ClienteHomeFragment : Fragment() {

    private var _binding: FragmentClienteHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var businessAdapter: BusinessAdapter
    private lateinit var clientBookingAdapter: ClientBookingAdapter
    private val businessList = mutableListOf<Business>()
    private val clientViewModel: ClientViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteHomeBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    // No método onViewCreated, inicie o carregamento imediato dos dados
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        setupAdapters()
        fetchBusinesses()
        setupSearchView()

        // Mostra o loading até carregar os agendamentos
        binding.progressBar.visibility = View.VISIBLE

        // Obtém o ID do usuário e inicia o carregamento dos agendamentos e dados do usuário
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            clientViewModel.loadUserData(it)
            clientViewModel.fetchUserBookings(it)  // Carrega agendamentos via ViewModel
        }

        observeUserData()
    }

    private fun setupAdapters() {
        val categories =
            listOf("Cabeleireiro", "Manicure", "Estética", "Barbeiro", "Massagem")
        _binding?.categoriesRecyclerView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        _binding?.categoriesRecyclerView?.adapter =
            CategoriesPagerAdapter(categories) { selectedCategory ->
                filterBusinessesByCategory(selectedCategory)
            }

        businessAdapter = BusinessAdapter(requireContext(), businessList) { business ->
            fetchBusinessIdAndOpenDetails(business.name)
        }

        _binding?.establishmentsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        _binding?.establishmentsRecyclerView?.adapter = businessAdapter
    }

    private fun fetchBusinesses() {
        firestore.collection("business")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (isAdded && _binding != null) {
                    if (!querySnapshot.isEmpty) {
                        businessList.clear()
                        businessList.addAll(querySnapshot.toObjects(Business::class.java))
                        updateBusinessAdapter(businessList)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClienteHomeFragment", "Erro ao buscar estabelecimentos: ${e.message}")
            }
    }

    private fun setupClientBookingAdapter(bookings: List<Booking>) {
        clientBookingAdapter = ClientBookingAdapter(
            context = requireContext(),
            bookings = bookings,
            onConfirmClick = { booking -> clientViewModel.confirmBooking(requireContext(), booking) },
            onCancelClick = { booking -> clientViewModel.cancelBooking(requireContext(), booking) },
            onRateClick = { booking -> showRatingPopup(booking) },
            onEmptyList = { showNoBookingsMessage(true) }  // Exibe mensagem de lista vazia
        )

        binding.todayBookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.todayBookingsRecyclerView.adapter = clientBookingAdapter
    }


    private fun showDefaultView() {
        binding.apply {
            todayAgendaCardView.visibility = View.VISIBLE  // Mostra agendamentos
            categoriesRecyclerView.visibility = View.VISIBLE  // Mostra categorias
            establishmentsRecyclerView.visibility = View.VISIBLE  // Mostra estabelecimentos
            categoriesTitle.visibility = View.VISIBLE  // Mostra o título "Categorias"
        }
    }

    private fun showRatingPopup(booking: Booking) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_rating, null)
        val ratingQuality = dialogView.findViewById<RatingBar>(R.id.ratingQuality)
        val ratingPunctuality = dialogView.findViewById<RatingBar>(R.id.ratingPunctuality)
        val ratingService = dialogView.findViewById<RatingBar>(R.id.ratingService)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        saveButton.setOnClickListener {
            val qualityRating = ratingQuality.rating.toInt()
            val punctualityRating = ratingPunctuality.rating.toInt()
            val serviceRating = ratingService.rating.toInt()
            clientViewModel.saveRatings(requireContext(), booking.id, qualityRating, punctualityRating, serviceRating)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun observeUserData() {
        clientViewModel.userName.observe(viewLifecycleOwner) { userName ->
            _binding?.apply {
                binding.userName.text = userName ?: "Nome do Usuário"
                updateGreeting()
            }
        }

        clientViewModel.userImage.observe(viewLifecycleOwner) { imageUrl ->
            _binding?.let { binding ->
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.foto_perfil_generica)
                    .error(R.drawable.foto_perfil_generica)
                    .into(binding.userImage)
            }
        }

        clientViewModel.todayBookings.observe(viewLifecycleOwner) { bookings ->
            binding.progressBar.visibility = View.GONE  // Esconde o loading quando dados são carregados
            if (bookings.isNotEmpty()) {
                setupClientBookingAdapter(bookings)
                showNoBookingsMessage(false)
            } else {
                showNoBookingsMessage(true)
            }
        }
    }

    private fun filterBusinessesByCategory(category: String) {
        val filteredBusinesses = businessList.filter { it.serviceType == category }
        updateBusinessAdapter(filteredBusinesses)
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

        binding.searchView.setOnCloseListener {
            showDefaultView()
            false
        }
    }

    private fun filterBusinesses(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            businessList
        } else {
            businessList.filter { it.name.contains(query, ignoreCase = true) }
        }

        updateBusinessAdapter(filteredList)

        if (query.isNullOrEmpty()) {
            showDefaultView()
        } else {
            showOnlyBusinesses()
        }
    }

    private fun showOnlyBusinesses() {
        _binding?.apply {
            todayAgendaCardView.visibility = View.GONE
            categoriesRecyclerView.visibility = View.GONE
            categoriesTitle.visibility = View.GONE
            establishmentsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateBusinessAdapter(filteredBusinesses: List<Business>) {
        if (isAdded && _binding != null) {
            businessAdapter = BusinessAdapter(requireContext(), filteredBusinesses) { business ->
                fetchBusinessIdAndOpenDetails(business.name)
            }
            binding.establishmentsRecyclerView.adapter = businessAdapter
            businessAdapter.notifyDataSetChanged()
        }
    }

    private fun fetchBusinessIdAndOpenDetails(businessName: String) {
        firestore.collection("business")
            .whereEqualTo("name", businessName)
            .get()
            .addOnSuccessListener { documents ->
                documents.firstOrNull()?.id?.let { businessId ->
                    val intent =
                        Intent(requireContext(), EmpresaDetalhesActivity::class.java).apply {
                            putExtra("companyId", businessId)
                        }
                    startActivity(intent)
                }
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

    private fun showNoBookingsMessage(show: Boolean) {
        if (_binding == null || !isAdded) return

        binding.noBookingsMessage.visibility = if (show) View.VISIBLE else View.GONE
        binding.noBookingsImage.visibility = if (show) View.VISIBLE else View.GONE
        binding.todayBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Evita memory leaks
    }
}
