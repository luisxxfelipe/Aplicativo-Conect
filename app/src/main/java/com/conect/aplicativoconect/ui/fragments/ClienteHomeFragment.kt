package com.conect.aplicativoconect.ui.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Booking
import com.conect.aplicativoconect.data.models.Business
import com.conect.aplicativoconect.databinding.FragmentClienteHomeBinding
import com.conect.aplicativoconect.ui.activities.EmpresaDetalhesActivity
import com.conect.aplicativoconect.ui.adapters.BusinessAdapter
import com.conect.aplicativoconect.ui.adapters.CategoriesPagerAdapter
import com.conect.aplicativoconect.ui.adapters.ClientBookingAdapter
import com.conect.aplicativoconect.ui.viewmodels.ClientViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.conect.aplicativoconect.utils.Validator
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ClienteHomeFragment : Fragment() {

    private var _binding: FragmentClienteHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var businessAdapter: BusinessAdapter
    private lateinit var clientBookingAdapter: ClientBookingAdapter
    private val businessList = mutableListOf<Business>()
    
    // ActivityResultLauncher para permissions
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchUserLocation(false)
        } else {
            Log.d("ClienteHomeFragment", "Permissão de localização negada")
        }
    }
    private val clientViewModel: ClientViewModel by activityViewModels()
    private var selectedCategory: String? = null
    private val PAGE_SIZE = 4 // Número de itens por vez
    private var lastVisible: DocumentSnapshot? = null // Último documento carregado
    private var isLoading = false // Controle de carregamento
    private val cacheKey = "business_cache"
    private var currentCity: String? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1


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

        // Carregar cache
        val cachedBusinesses = loadCachedBusinesses()
        if (cachedBusinesses.isNotEmpty()) {
            updateBusinessAdapter(cachedBusinesses)
        }

        setupSearchView()

        binding.progressBar.visibility = View.VISIBLE

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            clientViewModel.loadUserData(it)
            clientViewModel.fetchUserBookings(it)
        }

        // Receber a flag para forçar atualização
        val forceUpdate = requireActivity().intent.getBooleanExtra("FORCE_UPDATE", false)

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchUserLocation(forceUpdate) // Passa a flag para forçar a atualização
        }


        observeUserData()
    }


    // ✅ OTIMIZADO: Função consolidada para cache de businesses
    private fun cacheBusinesses(businesses: List<Business> = businessList) {
        val sharedPreferences = 
            requireContext().getSharedPreferences(cacheKey, Context.MODE_PRIVATE)
        val json = Gson().toJson(businesses)
        sharedPreferences.edit().putString(cacheKey, json).apply()
    }

    private fun loadCachedBusinesses(): List<Business> {
        val sharedPreferences =
            requireContext().getSharedPreferences(cacheKey, Context.MODE_PRIVATE)
        val cachedData = sharedPreferences.getString(cacheKey, null)

        return if (!cachedData.isNullOrEmpty()) {
            Gson().fromJson(cachedData, object : TypeToken<List<Business>>() {}.type)
        } else {
            emptyList()
        }
    }



    private fun fetchUserLocation(forceUpdate: Boolean = false) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("ClienteHomeFragment", "Permissão de localização não concedida.")
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        val locationProvider = LocationServices.getFusedLocationProviderClient(requireContext())
        locationProvider.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val city = getCityFromLocation(location.latitude, location.longitude)
                    withContext(Dispatchers.Main) {
                        if (city == "Cidade não encontrada" || city == "Sem nome de cidade") {
                            Log.e("ClienteHomeFragment", "Não foi possível determinar a cidade.")
                            showFallbackLocation(location.latitude, location.longitude)
                        } else {
                            val isCityChanged = currentCity != city
                            currentCity = city

                            // Atualiza somente se a cidade mudou ou se a flag FORCE_UPDATE está ativa
                            if (isCityChanged || forceUpdate) {
                                fetchBusinessesByCity(city)
                            }
                        }
                    }
                }
            } else {
                Log.e("ClienteHomeFragment", "Localização não disponível.")
            }
        }.addOnFailureListener {
            Log.e("ClienteHomeFragment", "Erro ao obter localização: ${it.message}")
        }
    }


    private fun showFallbackLocation(latitude: Double, longitude: Double) {
        AlertDialog.Builder(requireContext())
            .setTitle("Localização não encontrada")
            .setMessage("Não foi possível determinar a cidade. Suas coordenadas são:\nLatitude: $latitude\nLongitude: $longitude")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }


    private fun getCityFromLocation(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: "Sem nome de cidade"
                city
            } else {
                "Cidade não encontrada"
            }
        } catch (e: Exception) {
            Log.e("ClienteHomeFragment", "Erro no Geocoder: ${e.message}")
            "Erro ao buscar cidade"
        }
    }

    private fun fetchBusinessesByCity(city: String) {
        if (isLoading) return
        isLoading = true

        val normalizedCity = city.trim().lowercase(Locale.getDefault())

        // ✅ OTIMIZAÇÃO: Query direta com filtro no Firestore + limite
        val query = firestore.collection("business")
            .whereEqualTo("city", normalizedCity)
            .limit(PAGE_SIZE.toLong())

        // Aplicar paginação se houver documento anterior
        lastVisible?.let { query.startAfter(it) }

        query.get()
            .addOnSuccessListener { querySnapshot ->
                val businesses = querySnapshot.toObjects(Business::class.java)
                
                // Atualizar lastVisible para próxima página
                if (querySnapshot.documents.isNotEmpty()) {
                    lastVisible = querySnapshot.documents.lastOrNull()
                }

                // Se é a primeira busca, limpar a lista
                if (lastVisible == null) {
                    businessList.clear()
                }
                
                businessList.addAll(businesses)
                cacheBusinesses() // ✅ OTIMIZADO: Usa função consolidada
                updateBusinessAdapter(businessList)
            }
            .addOnFailureListener {
                Log.e("ClienteHomeFragment", "Erro ao buscar empresas: ${it.message}")
            }
            .addOnCompleteListener {
                isLoading = false
            }
    }

    private fun setupAdapters() {
        // Inicializa o adapter
        businessAdapter = BusinessAdapter(requireContext()) { business ->
            fetchBusinessIdAndOpenDetails(business.name)
        }

        // Configura o RecyclerView com scroll infinito
        val layoutManager = LinearLayoutManager(requireContext())
        binding.establishmentsRecyclerView.apply {
            this.layoutManager = layoutManager
            setHasFixedSize(true)
            adapter = businessAdapter
            
            // ✅ IMPLEMENTAR SCROLL INFINITO
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    // Carregar mais quando próximo do fim (últimos 3 itens)
                    if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                        fetchBusinesses(currentCity, false) // Carregar próxima página
                    }
                }
            })
        }

        // Configura o RecyclerView de categorias
        val categories =  listOf("Cabeleireiro", "Manicure", "Estética", "Barbeiro", "Massagem", "Técnico de Informática", "Fotógrafo", "Depilação", "Desenvolvedor de sites")
        binding.categoriesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesRecyclerView.adapter =
            CategoriesPagerAdapter(categories) { selectedCategory ->
                if (selectedCategory != null) {
                    filterBusinessesByCategory(selectedCategory)
                } else {
                    // Caso o filtro seja removido (categoria desmarcada), mostra todos os negócios
                    updateBusinessAdapter(businessList)
                }
            }
    }


    private fun fetchBusinesses(city: String? = currentCity, isFirstLoad: Boolean = false) {
        if (isLoading) return

        isLoading = true
        lifecycleScope.launch {
            try {
                val querySnapshot = withContext(Dispatchers.IO) {
                    var query = firestore.collection("business")
                        .limit(PAGE_SIZE.toLong())
                    
                    // Aplicar filtro de cidade se especificado
                    city?.let { 
                        query = query.whereEqualTo("city", it.trim().lowercase(Locale.getDefault()))
                    }
                    
                    // Aplicar paginação apenas se não for primeira carga
                    if (!isFirstLoad) {
                        lastVisible?.let { query = query.startAfter(it) }
                    }
                    
                    query.get().await()
                }

                val businesses = querySnapshot.toObjects(Business::class.java)
                
                // Atualizar lastVisible para próxima página
                if (querySnapshot.documents.isNotEmpty()) {
                    lastVisible = querySnapshot.documents.last()
                }

                if (businesses.isNotEmpty()) {
                    // Se é primeira carga, limpar lista
                    if (isFirstLoad) {
                        businessList.clear()
                    }
                    
                    // Evitar duplicatas
                    val uniqueBusinesses = businesses.filter { newBusiness ->
                        businessList.none { it.ownerId == newBusiness.ownerId }
                    }
                    
                    businessList.addAll(uniqueBusinesses)
                    updateBusinessAdapter(businessList)
                    cacheBusinesses(businessList)
                }
            } catch (e: Exception) {
                Log.e("ClienteHomeFragment", "Erro ao buscar empresas: ${e.message}")
            } finally {
                isLoading = false
            }
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
                onEmptyList = { showNoBookingsMessage(true) },
                onCardClick = { companyId -> openCompanyDetails(companyId) } // Adiciona lógica de clique no card
            )
            binding.todayBookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.todayBookingsRecyclerView.adapter = clientBookingAdapter
        } else {
            clientBookingAdapter.updateBookings(bookings)
        }
    }

    private fun openCompanyDetails(companyId: String) {
        val intent = Intent(requireContext(), EmpresaDetalhesActivity::class.java).apply {
            putExtra("companyId", companyId)
        }
        startActivity(intent)
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
            binding.progressBar.visibility = View.GONE
            val currentTime = Calendar.getInstance().time

            // Filtra apenas agendamentos futuros ou do dia atual
            val upcomingBookings = bookings.filter { booking ->
                val bookingDateTime = convertToDate(booking.date, booking.hour)
                bookingDateTime?.after(currentTime) ?: false
            }

            if (upcomingBookings.isNotEmpty()) {
                setupClientBookingAdapter(upcomingBookings)
                showNoBookingsMessage(false)
            } else {
                showNoBookingsMessage(true)
            }
        }

        // Observa mudanças na ação de confirmar/cancelar
        clientViewModel.refreshBookings.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                clientViewModel.fetchUserBookings(FirebaseAuth.getInstance().currentUser?.uid ?: "")
            }
        }
    }

    private fun convertToDate(date: String?, hour: String?): Date? {
        if (date == null || hour == null) return null
        return try {
            val dateTimeString = "$date $hour" // Combina data e hora
            Validator.DATE_TIME_FORMAT.parse(dateTimeString) // ✅ OTIMIZADO: Formatador central
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
    }

    private fun filterBusinesses(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            businessList
        } else {
            businessList.filter { it.name.contains(query, ignoreCase = true) }
        }
        businessAdapter.submitList(filteredList)
    }

    private fun updateBusinessAdapter(filteredBusinesses: List<Business>) {
        if (::businessAdapter.isInitialized) {
            businessAdapter.submitList(ArrayList(filteredBusinesses)) // Nova instância
            binding.establishmentsRecyclerView.adapter = businessAdapter // Garante o vínculo
        } else {
            Log.e("ClienteHomeFragment", "Adapter não inicializado!")
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

        // Verifica se a flag FORCE_UPDATE está presente e força a atualização
        val forceUpdate = requireActivity().intent.getBooleanExtra("FORCE_UPDATE", false)
        if (forceUpdate) {
            fetchUserLocation(forceUpdate = true)

            // Remove a flag para evitar múltiplas atualizações desnecessárias
            requireActivity().intent.removeExtra("FORCE_UPDATE")
        }
    }

}
