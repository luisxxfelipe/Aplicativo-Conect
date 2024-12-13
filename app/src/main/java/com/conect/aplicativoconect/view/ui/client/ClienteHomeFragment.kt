package com.conect.aplicativoconect.view.ui.client

import CategoriesPagerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentClienteHomeBinding
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.ui.admin.BusinessAdapter
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ClienteHomeFragment : Fragment() {

    private var _binding: FragmentClienteHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var businessAdapter: BusinessAdapter
    private lateinit var clientBookingAdapter: ClientBookingAdapter
    private val businessList = mutableListOf<Business>()
    private val clientViewModel: ClientViewModel by activityViewModels()
    private var selectedCategory: String? = null
    private val PAGE_SIZE = 4 // Número de itens por vez
    private var lastVisible: DocumentSnapshot? = null // Último documento carregado
    private var isLoading = false // Controle de carregamento


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteHomeBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    // No metodo onViewCreated, inicie o carregamento imediato dos dados
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
        val categories = listOf("Cabeleireiro", "Manicure", "Estética", "Barbeiro", "Massagem")
        _binding?.categoriesRecyclerView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        _binding?.categoriesRecyclerView?.adapter =
            CategoriesPagerAdapter(categories) { selectedCategory ->
                if (selectedCategory != null) {
                    filterBusinessesByCategory(selectedCategory)
                } else {
                    // Caso o filtro seja removido (categoria desmarcada), mostra todos os negócios
                    updateBusinessAdapter(businessList)
                }
            }

        businessAdapter = BusinessAdapter(requireContext()) { business ->
            fetchBusinessIdAndOpenDetails(business.name)
        }

        _binding?.establishmentsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        _binding?.establishmentsRecyclerView?.adapter = businessAdapter

        _binding?.establishmentsRecyclerView?.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (totalItemCount <= lastVisibleItemPosition + 4 && !isLoading) {
                    fetchBusinesses()
                }
            }
        })
    }


    private fun fetchBusinesses() {
        if (isLoading) return  // Evita múltiplas requisições simultâneas

        isLoading = true  // Inicia o carregamento
        var query = firestore.collection("business")
            .limit(PAGE_SIZE.toLong())  // Aumente para um número maior

        // Se já carregamos algum item, usamos startAfter para continuar de onde paramos
        lastVisible?.let {
            query = query.startAfter(it)
        }

        // Inicia a consulta
        query.get()
            .addOnSuccessListener { querySnapshot ->
                if (isAdded && _binding != null) {
                    if (!querySnapshot.isEmpty) {
                        val businesses = querySnapshot.toObjects(Business::class.java)
                        businessList.addAll(businesses)  // Adiciona os novos estabelecimentos à lista
                        lastVisible = querySnapshot.documents.last()  // Atualiza o último documento
                        updateBusinessAdapter(businessList)  // Atualiza o adaptador com a nova lista
                    }
                }
                isLoading = false  // Finaliza o carregamento
            }
            .addOnFailureListener { e ->
                isLoading = false  // Finaliza o carregamento em caso de erro
            }
    }


    private fun setupClientBookingAdapter(bookings: List<Booking>) {
        if (!::clientBookingAdapter.isInitialized) {
            clientBookingAdapter = ClientBookingAdapter(
                context = requireContext(),
                bookings = bookings,
                onConfirmClick = { booking ->
                    clientViewModel.confirmBooking(booking, requireContext())
                },
                onCancelClick = { booking ->
                    clientViewModel.cancelBooking(
                        booking,
                        requireContext()
                    )
                },
                onRateClick = { booking -> showRatingPopup(booking) },
                onEmptyList = { showNoBookingsMessage(true) }
            )
            binding.todayBookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.todayBookingsRecyclerView.adapter = clientBookingAdapter
        } else {
            clientBookingAdapter.updateBookings(bookings) // Certifique-se de que este método está no adapter
        }
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

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        saveButton.setOnClickListener {
            val qualityRating = ratingQuality.rating.toInt()
            val punctualityRating = ratingPunctuality.rating.toInt()
            val serviceRating = ratingService.rating.toInt()
            clientViewModel.saveRatings(
                requireContext(),
                booking.id,
                qualityRating,
                punctualityRating,
                serviceRating
            )
            dialog.dismiss()
        }

        dialog.show()

        // Ajuste da cor da fonte do botão "Salvar"
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.roxo))
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

        // Observa os agendamentos
        clientViewModel.todayBookings.observe(viewLifecycleOwner) { bookings ->
            binding.progressBar.visibility = View.GONE // Esconde o loading
            if (bookings.isNotEmpty()) {
                setupClientBookingAdapter(bookings)
                showNoBookingsMessage(false) // Oculta mensagem de "nenhum agendamento"
            } else {
                showNoBookingsMessage(true) // Exibe mensagem e imagem
            }
        }

        // Observa mudanças na ação de confirmar/cancelar
        clientViewModel.refreshBookings.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                clientViewModel.fetchUserBookings(FirebaseAuth.getInstance().currentUser?.uid ?: "")
            }
        }
    }


    private fun filterBusinessesByCategory(category: String) {
        if (selectedCategory == category) {
            // Se a mesma categoria for clicada novamente, desfaz o filtro
            selectedCategory = null
            updateBusinessAdapter(businessList) // Mostra todas as empresas
        } else {
            // Aplica o filtro pela nova categoria
            selectedCategory = category
            val filteredBusinesses = businessList.filter { it.serviceType == category }
            updateBusinessAdapter(filteredBusinesses)
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
            // Verifica se a lista é diferente da anterior para evitar updates desnecessários
            if (filteredBusinesses != businessAdapter.currentList) {
                businessAdapter.submitList(filteredBusinesses)
            }
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

    override fun onResume() {
        super.onResume()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            clientViewModel.fetchUserBookings(it) // Recarrega os agendamentos
        }
    }

}
