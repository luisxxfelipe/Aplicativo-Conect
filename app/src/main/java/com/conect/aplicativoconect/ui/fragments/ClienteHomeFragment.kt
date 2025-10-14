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
import android.widget.TextView

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
import com.conect.aplicativoconect.ui.viewmodels.ReferralViewModel
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
import kotlin.math.*

class ClienteHomeFragment : Fragment() {

    private var _binding: FragmentClienteHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var businessAdapter: BusinessAdapter
    private lateinit var clientBookingAdapter: ClientBookingAdapter
    private val businessList = mutableListOf<Business>()
    
    // 📍 LOCALIZAÇÃO: Permissão já solicitada na tela de boas-vindas
    private val clientViewModel: ClientViewModel by activityViewModels()
    private val referralViewModel: ReferralViewModel by activityViewModels()
    private var selectedCategory: String? = null
    private val PAGE_SIZE = 4 // Número de itens por vez
    private var lastVisible: DocumentSnapshot? = null // Último documento carregado
    private var isLoading = false // Controle de carregamento
    private var hasInitialLoadCompleted = false // 🆕 CONTROLE: Primeira carga completa
    private val cacheKey = "business_cache"
    private var currentCity: String? = null

    
    // 🚀 MELHORIAS: Propriedades para filtro por proximidade
    private var currentUserLat: Double? = null
    private var currentUserLon: Double? = null
    private val DEFAULT_RADIUS_KM = 15.0 // Raio padrão de 15km
    
    // 🎯 Cache inteligente com dados de localização
    private data class BusinessCache(
        val businesses: List<Business>,
        val timestamp: Long,
        val city: String?,
        val userLat: Double?,
        val userLon: Double?,
        val radiusKm: Double
    )
    
    // 🚀 OTIMIZAÇÃO: Cache inteligente de localização
    private fun getCachedUserCity(): String? {
        val prefs = requireContext().getSharedPreferences("location_cache", Context.MODE_PRIVATE)
        val cachedCity = prefs.getString("user_city", null)
        val cacheTime = prefs.getLong("cache_time", 0)
        val now = System.currentTimeMillis()
        
        // Cache válido por 24 horas
        return if (now - cacheTime < 24 * 60 * 60 * 1000) cachedCity else null
    }
    
    private fun saveUserCity(city: String) {
        val prefs = requireContext().getSharedPreferences("location_cache", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_city", city)
            .putLong("cache_time", System.currentTimeMillis())
            .apply()
    }
    
    // 🎯 NOVA FUNÇÃO: Calcular distância entre duas coordenadas (Haversine formula)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Raio da Terra em km
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    // 🚀 CACHE INTELIGENTE: Cache baseado em localização e proximidade
    private fun getCachedBusinessesForLocation(city: String?, lat: Double?, lon: Double?): List<Business>? {
        val prefs = requireContext().getSharedPreferences("business_location_cache", Context.MODE_PRIVATE)
        val cacheJson = prefs.getString("cache_data", null) ?: return null
        
        return try {
            val cache = Gson().fromJson(cacheJson, BusinessCache::class.java)
            val now = System.currentTimeMillis()
            
            // Cache válido por 2 horas para estabelecimentos
            val isExpired = now - cache.timestamp > 2 * 60 * 60 * 1000
            
            // Verificar se localização mudou significativamente (mais de 5km)
            val locationChanged = if (lat != null && lon != null && cache.userLat != null && cache.userLon != null) {
                calculateDistance(lat, lon, cache.userLat, cache.userLon) > 5.0
            } else {
                city != cache.city
            }
            
            if (!isExpired && !locationChanged) {
                cache.businesses
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ClienteHomeFragment", "Erro ao ler cache de estabelecimentos: ${e.message}")
            null
        }
    }
    
    // 🎯 SALVAR CACHE: Cache com informações de localização
    private fun saveBusinessesCache(businesses: List<Business>, city: String?, lat: Double?, lon: Double?) {
        val cache = BusinessCache(
            businesses = businesses,
            timestamp = System.currentTimeMillis(),
            city = city,
            userLat = lat,
            userLon = lon,
            radiusKm = DEFAULT_RADIUS_KM
        )
        
        val prefs = requireContext().getSharedPreferences("business_location_cache", Context.MODE_PRIVATE)
        prefs.edit().putString("cache_data", Gson().toJson(cache)).apply()
        
    }
    
    // 🎯 FUNÇÃO UTILITÁRIA: Formatar texto de distância
    private fun formatDistanceText(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()}m"
            distanceKm < 10.0 -> String.format("%.1fkm", distanceKm)
            else -> "${distanceKm.toInt()}km"
        }
    }
    
    // 🔄 TENTATIVA SEM FILTRO DE CIDADE: Buscar todos os estabelecimentos ativos
    private fun makeSecondAttempt(city: String?) {
        lifecycleScope.launch {
            try {
                val querySnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("business")
                        .whereEqualTo("isActive", true)
                        .limit(50) // Buscar mais estabelecimentos
                        .get()
                        .await()
                }

                val businesses = querySnapshot.toObjects(Business::class.java)
                
                if (businesses.isNotEmpty()) {
                    businessList.clear()
                    businessList.addAll(businesses)
                    updateBusinessAdapter(businessList)
                    cacheBusinesses(businessList)
                } else {
                    // Nem na segunda tentativa encontrou - mostrar mensagem promocional
                    showPromotionalMessage(city)
                }
            } catch (e: Exception) {
                showPromotionalMessage(city)
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    // 🚀 MENSAGENS PROMOCIONAIS para diferentes cenários
    
    private fun showPromotionalMessage(city: String?) {
        showNoEstablishmentsMessage(true, customMessage = true)
        binding.noEstablishmentsTitle.text = "Nenhum estabelecimento encontrado"
        
        val locationText = if (city != null) "em $city" else "na sua região"
        
        binding.noEstablishmentsMessage.text = "Ainda não temos estabelecimentos cadastrados $locationText.\n\nQue tal ajudar a crescer nossa comunidade?"
        binding.progressBar.visibility = View.GONE
        
        // 🚀 MOSTRAR POPUP DE INDICAÇÕES
        showReferralPopup(city)
    }
    
    // 🎯 POPUP DE PROGRAMA DE INDICAÇÕES
    private fun showReferralPopup(city: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // 🔒 CONTROLE DIÁRIO: Verificar se deve mostrar popup
        lifecycleScope.launch {
            val shouldShow = referralViewModel.shouldShowReferralPopup(userId)
            
            if (!shouldShow) {
                return@launch
            }
            
            withContext(Dispatchers.Main) {
                val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_referral_program, null)
                
                val btnShareWhatsApp = dialogView.findViewById<Button>(R.id.btnShareWhatsApp)
                val btnViewPoints = dialogView.findViewById<Button>(R.id.btnViewPoints)
                val btnClose = dialogView.findViewById<TextView>(R.id.btnClose)

                val dialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(true)
                    .create()

                // Configurar ações dos botões
                btnShareWhatsApp.setOnClickListener {
                    shareReferralWhatsApp()
                    
                    // 🔒 CONTROLE DIÁRIO: Marcar popup como mostrado
                    lifecycleScope.launch {
                        referralViewModel.updateLastPopupShown(userId)
                    }
                    
                    dialog.dismiss()
                }

                btnViewPoints.setOnClickListener {
                    // 🎯 Navegar para a tela dedicada de pontos
                    val intent = Intent(requireContext(), com.conect.aplicativoconect.ui.activities.ReferralPointsActivity::class.java)
                    startActivity(intent)
                    dialog.dismiss()
                }

                btnClose.setOnClickListener {
                    // 🔒 CONTROLE DIÁRIO: Marcar popup como mostrado mesmo se fechado
                    lifecycleScope.launch {
                        referralViewModel.updateLastPopupShown(userId)
                    }
                    
                    dialog.dismiss()
                }

                dialog.show()
                
                // Garantir fundo transparente
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
    }
    
    // 📱 COMPARTILHAR NO WHATSAPP
    private fun shareReferralWhatsApp() {
        try {
            val userName = clientViewModel.userName.value ?: "Um usuário"
            val message = referralViewModel.generateReferralMessage(userName)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
            }
            
            startActivity(Intent.createChooser(shareIntent, "Compartilhar via WhatsApp"))
        } catch (e: Exception) {
            // Fallback para compartilhamento geral se WhatsApp não estiver instalado
            val userName = clientViewModel.userName.value ?: "Um usuário"
            val message = referralViewModel.generateReferralMessage(userName)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Compartilhar"))
        }
    }
    
    // 🆕 NOVA FUNÇÃO: Solicitar localização para atualizar cache (SEM INTERRUPÇÃO)
    private fun requestLocationForCacheUpdate(userId: String?) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 🎯 SEM PERMISSÃO: Carregar estabelecimentos SEM filtro de localização
            Log.d("ClienteHomeFragment", "📍 Sem permissão de localização - carregando todos os estabelecimentos")
            currentCity = null
            currentUserLat = null
            currentUserLon = null
            
            // Buscar estabelecimentos sem filtro de cidade
            fetchBusinessesWithoutLocation()
        } else {
            // Com permissão - buscar localização
            fetchUserLocationAndUpdate(userId)
        }
    }
    
    // 🆕 NOVA FUNÇÃO: Buscar estabelecimentos sem localização
    private fun fetchBusinessesWithoutLocation() {
        lifecycleScope.launch {
            try {
                val querySnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("business")
                        .whereEqualTo("isActive", true)
                        .limit(20) // Mais estabelecimentos já que não tem filtro de cidade
                        .get()
                        .await()
                }

                val businesses = querySnapshot.toObjects(Business::class.java)
                
                if (businesses.isNotEmpty()) {
                    businessList.clear()
                    businessList.addAll(businesses)
                    updateBusinessAdapter(businessList)
                    cacheBusinesses(businessList)
                    
                    Log.d("ClienteHomeFragment", "✅ Carregados ${businesses.size} estabelecimentos sem filtro de localização")
                } else {
                    showNoEstablishmentsMessage(true, customMessage = true)
                    binding.noEstablishmentsTitle.text = "Nenhum estabelecimento encontrado"
                    binding.noEstablishmentsMessage.text = "Não encontramos estabelecimentos disponíveis no momento."
                }
            } catch (e: Exception) {
                Log.e("ClienteHomeFragment", "Erro ao buscar estabelecimentos: ${e.message}")
                showNoEstablishmentsMessage(true, customMessage = true)
                binding.noEstablishmentsTitle.text = "Erro ao carregar"
                binding.noEstablishmentsMessage.text = "Ocorreu um erro ao carregar os estabelecimentos. Tente novamente."
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    hasInitialLoadCompleted = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // 🆕 NOVA FUNÇÃO: Buscar localização e atualizar cache + Firebase
    private fun fetchUserLocationAndUpdate(userId: String?) {
        if (isLoading) return
        
        val locationProvider = LocationServices.getFusedLocationProviderClient(requireContext())
        locationProvider.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // 🎯 ARMAZENAR coordenadas do usuário
                currentUserLat = location.latitude
                currentUserLon = location.longitude
                
                lifecycleScope.launch(Dispatchers.IO) {
                    val city = getCityFromLocation(location.latitude, location.longitude)
                    withContext(Dispatchers.Main) {
                        if (city != "Cidade não encontrada" && city != "Sem nome de cidade" && city != "Erro ao buscar cidade") {
                            currentCity = city
                            
                            // Salvar no cache local
                            saveUserCity(city)
                            
                            // Salvar no Firebase (novo campo)
                            userId?.let { saveUserCityToFirebase(it, city) }
                            
                            // 🚀 VERIFICAR CACHE antes de buscar no Firestore
                            val cachedBusinesses = getCachedBusinessesForLocation(city, currentUserLat, currentUserLon)
                            if (cachedBusinesses != null && cachedBusinesses.isNotEmpty()) {
                                updateBusinessAdapter(cachedBusinesses)
                                businessList.clear()
                                businessList.addAll(cachedBusinesses)
                            } else {
                                // Cache vazio ou expirado - buscar do Firestore
                                fetchBusinessesWithProximity(city, currentUserLat, currentUserLon, true)
                            }
                            
                        } else {
                            currentCity = null
                            currentUserLat = null
                            currentUserLon = null
                            // 🎯 Fallback: Buscar estabelecimentos sem localização
                            fetchBusinessesWithoutLocation()
                        }
                    }
                }
            } else {
                currentCity = null
                currentUserLat = null
                currentUserLon = null
                // 🎯 Fallback: Buscar estabelecimentos sem localização
                fetchBusinessesWithoutLocation()
            }
        }.addOnFailureListener { exception ->
            // Erro ao obter localização - fallback sem interromper o usuário
            Log.e("ClienteHomeFragment", "Erro ao obter localização: ${exception.message}")
            currentCity = null
            currentUserLat = null
            currentUserLon = null
            // 🎯 Fallback: Buscar estabelecimentos sem localização
            fetchBusinessesWithoutLocation()
        }
    }

    // 🆕 NOVA FUNÇÃO: Salvar cidade no Firebase
    private fun saveUserCityToFirebase(userId: String, city: String) {
        firestore.collection("users").document(userId)
            .update("city", city, "cityUpdatedAt", System.currentTimeMillis())
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteHomeBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    private fun setupReferralButton() {
        binding.referralButton.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userName = currentUser?.displayName ?: "Usuário ConecteX"
            val appUrl = "https://play.google.com/store/apps/details?id=com.conect.aplicativoconect"
            val message = "Olá! Eu uso o ConecteX para encontrar serviços próximos e queria te indicar.\n\n" +
                    "📱 O que é o ConecteX?\n" +
                    "• App para encontrar profissionais autônomos\n" +
                    "• Agendamento fácil e rápido\n" +
                    "• Avaliações reais dos clientes\n" +
                    "• Profissionais verificados\n\n" +
                    "✨ Se você é um profissional autônomo, pode se cadastrar e ganhar mais clientes!\n\n" +
                    "🎯 Categorias disponíveis:\n" +
                    "• Beleza (Cabeleireiro, Manicure, Estética)\n" +
                    "• Técnicos (Informática, Eletrônicos)\n" +
                    "• Serviços Gerais (Fotografia, Design)\n" +
                    "• E muito mais!\n\n" +
                    "📲 Baixe agora: $appUrl\n\n" +
                    "_Indicado por: $userName"

            val whatsappIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
            }

            try {
                startActivity(whatsappIntent)
            } catch (e: Exception) {
                // Fallback para compartilhamento geral
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                startActivity(Intent.createChooser(shareIntent, "Compartilhar indicação"))
            }
        }
    }

    // No metodo onViewCreated, inicie o carregamento imediato dos dados
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        setupAdapters()
        setupReferralButton()
        setupSearchView()

        binding.progressBar.visibility = View.VISIBLE

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            clientViewModel.loadUserData(it)
            clientViewModel.fetchUserBookings(it)
        }

        // Receber a flag para forçar atualização
        val forceUpdate = requireActivity().intent.getBooleanExtra("FORCE_UPDATE", false)

        // 🚀 ESTRATÉGIA CONTROLADA: Cache primeiro, busca apenas se necessário
        val cachedCity = getCachedUserCity()
        if (cachedCity != null && !forceUpdate) {
            // Verificar cache inteligente de estabelecimentos primeiro
            currentCity = cachedCity
            val smartCache = getCachedBusinessesForLocation(cachedCity, null, null)
            if (smartCache != null && smartCache.isNotEmpty()) {
                updateBusinessAdapter(smartCache)
                businessList.clear()
                businessList.addAll(smartCache)
                hasInitialLoadCompleted = true // 🆕 MARCAR: Cache como carga inicial completa
                 binding.progressBar.visibility = View.GONE
            } else {
                // Cache de estabelecimentos vazio - buscar localização atual
                requestLocationForCacheUpdate(userId)
            }
        } else {
            // Cache expirado ou forçar update - pedir localização
           requestLocationForCacheUpdate(userId)
        }


        observeUserData()
        
        // ❤️ Carregar favoritos do usuário
        loadUserFavorites()
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
        // 🎯 LOCALIZAÇÃO: Tenta usar se tiver permissão, senão funciona sem filtro
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Com permissão - usar localização
            fetchUserLocationAndUpdate(userId)
        } else {
            // Sem permissão - funcionar sem localização (já foi pedida na tela de boas-vindas)
            Log.d("ClienteHomeFragment", "📍 Sem permissão de localização - usando busca geral")
            fetchBusinessesWithoutLocation()
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

    private fun setupAdapters() {
        // Inicializa o adapter com callback de favoritos
        businessAdapter = BusinessAdapter(
            context = requireContext(),
            onBusinessClick = { business ->
                fetchBusinessIdAndOpenDetails(business.name)
            },
            onFavoriteClick = { business, isFavorite ->
                handleFavoriteClick(business, isFavorite)
            }
        )

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
                    
                    // 🔒 SCROLL INFINITO CONTROLADO: Carregar mais apenas se necessário
                    if (!isLoading && hasInitialLoadCompleted && totalItemCount > 0 && 
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 1) {
                        
                        // 🚀 Usar função com proximidade se temos coordenadas
                        if (currentUserLat != null && currentUserLon != null) {
                            fetchBusinessesWithProximity(currentCity, currentUserLat, currentUserLon, false)
                        } else {
                            fetchBusinesses(currentCity, false) // Fallback para função tradicional
                        }
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
        // 🔒 BLOQUEIO INTELIGENTE: Evitar múltiplas chamadas desnecessárias
        if (isLoading) {
            return
        }
        
        // 🔒 BLOQUEIO: Se já carregou dados e não é primeira carga, e tem dados, não recarregar
        if (!isFirstLoad && hasInitialLoadCompleted && businessList.isNotEmpty()) {
           return
        }

        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val querySnapshot = withContext(Dispatchers.IO) {
                    var query = firestore.collection("business")
                        .whereEqualTo("isActive", true) // 🚀 Filtro otimizado primeiro
                        .limit(PAGE_SIZE.toLong())
                    
                    // Aplicar filtro de cidade - tentar ambos os casos
                    city?.let { cityName ->
                        val trimmedCity = cityName.trim()
                        // Primeiro tentar com a capitalização original
                        query = query.whereEqualTo("city", trimmedCity)
                    }
                    
                    // Aplicar paginação apenas se não for primeira carga
                    if (!isFirstLoad) {
                        lastVisible?.let { query = query.startAfter(it) }
                    }
                    
                    query.get().await()
                }

                var businesses = querySnapshot.toObjects(Business::class.java)
                
                // Se não encontrou nada e tem cidade, tentar com diferentes capitalizações
                if (businesses.isEmpty() && !city.isNullOrBlank()) {
                    val cityVariations = listOf(
                        city.trim().toLowerCase(),
                        city.trim().toUpperCase(), 
                        city.trim().capitalize()
                    )
                    
                    for (variation in cityVariations) {
                        if (variation != city.trim()) {
                            val altQuery = firestore.collection("business")
                                .whereEqualTo("isActive", true)
                                .whereEqualTo("city", variation)
                                .limit(PAGE_SIZE.toLong())
                            val altSnapshot = altQuery.get().await()
                            businesses = altSnapshot.toObjects(Business::class.java)
                            
                            if (businesses.isNotEmpty()) {
                                break
                            }
                        }
                    }
                }
                
                // Se ainda não encontrou, tentar sem filtro de cidade
                if (businesses.isEmpty() && !city.isNullOrBlank()) {
                    val noFilterQuery = firestore.collection("business")
                        .whereEqualTo("isActive", true)
                        .limit(50)
                    val noFilterSnapshot = noFilterQuery.get().await()
                    businesses = noFilterSnapshot.toObjects(Business::class.java)
                }
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
                
                // 🔧 EXIBIR MENSAGEM DE ERRO personalizada
                withContext(Dispatchers.Main) {
                    if (businessList.isEmpty()) {
                        showNoEstablishmentsMessage(true, customMessage = true)
                        binding.noEstablishmentsTitle.text = "Erro ao carregar estabelecimentos"
                        binding.noEstablishmentsMessage.text = "Ocorreu um erro ao carregar os estabelecimentos.\nVerifique sua conexão e tente novamente."
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (isFirstLoad) {
                        hasInitialLoadCompleted = true // 🆕 MARCAR: Primeira carga completa
                    }
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // 🚀 NOVA FUNÇÃO: Buscar estabelecimentos com filtro por proximidade real
    private fun fetchBusinessesWithProximity(
        city: String? = currentCity, 
        userLat: Double? = currentUserLat, 
        userLon: Double? = currentUserLon, 
        isFirstLoad: Boolean = false,
        radiusKm: Double = DEFAULT_RADIUS_KM
    ) {
        // 🔒 BLOQUEIO INTELIGENTE: Evitar múltiplas chamadas
        if (isLoading) {
            return
        }
        
        // 🔒 BLOQUEIO: Se já carregou dados e não é primeira carga, e tem dados, não recarregar
        if (!isFirstLoad && hasInitialLoadCompleted && businessList.isNotEmpty()) {
            return
        }

        isLoading = true
        lifecycleScope.launch {
            try {
                val querySnapshot = withContext(Dispatchers.IO) {
                    var query = firestore.collection("business")
                        .whereEqualTo("isActive", true)
                        
                    // Se temos localização do usuário, buscar mais estabelecimentos para filtrar por proximidade
                    val queryLimit = if (userLat != null && userLon != null) {
                        (PAGE_SIZE * 3).toLong() // Buscar mais para filtrar por distância depois
                    } else {
                        PAGE_SIZE.toLong()
                    }
                    
                    query = query.limit(queryLimit)
                    
                    // Aplicar filtro de cidade se não temos coordenadas do usuário
                    if (userLat == null || userLon == null) {
                        city?.let { cityName ->
                            val trimmedCity = cityName.trim()
                            query = query.whereEqualTo("city", trimmedCity)
                        }
                    }
                    
                    // Aplicar paginação apenas se não for primeira carga
                    if (!isFirstLoad) {
                        lastVisible?.let { query = query.startAfter(it) }
                    }
                    
                    query.get().await()
                }

                var businesses = querySnapshot.toObjects(Business::class.java)
                
                // 🎯 FILTRAR POR PROXIMIDADE se temos localização do usuário
                if (userLat != null && userLon != null) {
                    businesses = businesses.filter { business ->
                        // Incluir estabelecimentos com coordenadas válidas dentro do raio
                        if (business.latitude != 0.0 && business.longitude != 0.0) {
                            val distance = calculateDistance(userLat, userLon, business.latitude, business.longitude)
                            val isWithinRadius = distance <= radiusKm
                            isWithinRadius
                        } else {
                            // Incluir estabelecimentos sem coordenadas da mesma cidade
                            city?.let { currentCity ->
                                business.city.trim().lowercase(Locale.getDefault()) == currentCity.trim().lowercase(Locale.getDefault())
                            } ?: true
                        }
                    }.sortedBy { business ->
                        // 🏆 ORDENAR por distância (mais próximos primeiro)
                        if (business.latitude != 0.0 && business.longitude != 0.0) {
                            calculateDistance(userLat, userLon, business.latitude, business.longitude)
                        } else {
                            Double.MAX_VALUE // Estabelecimentos sem coordenadas por último
                        }
                    }
                    
                    // Limitar ao PAGE_SIZE após filtro de proximidade
                    businesses = businesses.take(PAGE_SIZE)
                    
                }
                
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
                    
                    // 🎯 SALVAR no cache inteligente
                    saveBusinessesCache(businessList, city, userLat, userLon)
                } else if (isFirstLoad) {
                    // Se primeira carga e não encontrou nada, fazer segunda tentativa sem filtro de proximidade
                    makeSecondAttempt(city)
                    return@launch
                }
            } catch (e: Exception) {
                
                // Fallback para busca tradicional por cidade
                if (isFirstLoad) {
                    fetchBusinesses(city, true)
                } else {
                    withContext(Dispatchers.Main) {
                        if (businessList.isEmpty()) {
                            showNoEstablishmentsMessage(true, customMessage = true)
                            binding.noEstablishmentsTitle.text = "Erro ao carregar estabelecimentos"
                            binding.noEstablishmentsMessage.text = "Ocorreu um erro ao carregar os estabelecimentos.\nVerifique sua conexão e tente novamente."
                        }
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    
                    // 🎯 SEMPRE MOSTRAR POPUP DE INDICAÇÕES (independente se tem estabelecimentos)
                    if (isFirstLoad) {
                        showReferralPopup(city)
                    }
                }
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
                // ⚡ FIX: Usar ImageHelper otimizado
                com.conect.aplicativoconect.utils.ImageHelper.loadProfileImage(
                    requireContext(), 
                    imageUrl, 
                    binding.userImage
                )
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
            Log.d("ClienteHomeFragment", "🔄 Removendo filtro, mostrando ${businessList.size} estabelecimentos")
            updateBusinessAdapter(businessList) // Mostra todas as empresas
        } else {
            // Aplica o filtro pela nova categoria
            selectedCategory = category
            val filteredBusinesses = businessList.filter { it.serviceType == category }
            Log.d("ClienteHomeFragment", "🎯 Filtrando por '$category', encontrou ${filteredBusinesses.size} estabelecimentos")
            filteredBusinesses.forEach { business ->
                Log.d("ClienteHomeFragment", "✅ Filtrado: ${business.name} - Tipo: ${business.serviceType}")
            }
            updateBusinessAdapter(filteredBusinesses)
        }
    }


    private fun setupSearchView() {
        // 🔧 MELHORIA: Configurar SearchView para ser mais acessível
        binding.searchView.apply {
            // Permitir clique em qualquer lugar da SearchView
            setIconifiedByDefault(false)
            isFocusable = true
            isClickable = true
            
            // Expandir automaticamente quando tocada
            setOnClickListener {
                isIconified = false
                requestFocus()
            }
            
            // Melhorar acessibilidade
            setOnQueryTextFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    isIconified = false
                }
            }
        }
        
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterBusinesses(query)
                // 🔧 MELHORIA: Fechar teclado após busca
                binding.searchView.clearFocus()
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
        // 🚀 Atualizar com função que calcula distâncias
        businessAdapter.setUserLocation(currentUserLat, currentUserLon)
        businessAdapter.submitBusinessList(filteredList)
        
        // 🎨 CONTROLAR VISIBILIDADE na busca também
        showNoEstablishmentsMessage(filteredList.isEmpty())
        
        // 🔧 MENSAGEM ESPECÍFICA para busca
        if (filteredList.isEmpty() && !query.isNullOrEmpty()) {
            binding.noEstablishmentsTitle.text = "Nenhum resultado encontrado"
            binding.noEstablishmentsMessage.text = "Não encontramos estabelecimentos com o nome \"$query\".\nTente usar outras palavras-chave."
        } else if (filteredList.isEmpty()) {
            binding.noEstablishmentsTitle.text = "Nenhum estabelecimento encontrado"
        }
    }

    private fun updateBusinessAdapter(filteredBusinesses: List<Business>) {
        
        if (::businessAdapter.isInitialized) {
            // 🚀 Atualizar localização do usuário no adapter
            businessAdapter.setUserLocation(currentUserLat, currentUserLon)
            
            // ⭐ ORDENAR: Favoritos primeiro, depois por distância
            val sortedBusinesses = sortBusinessesByFavorites(filteredBusinesses)
            
            // 🎯 Usar nova função que calcula distâncias automaticamente
            businessAdapter.submitBusinessList(sortedBusinesses)
            binding.establishmentsRecyclerView.adapter = businessAdapter // Garante o vínculo
            
            val shouldShowMessage = filteredBusinesses.isEmpty()
            showNoEstablishmentsMessage(shouldShowMessage)
            
            // Forçar atualização da interface
            binding.establishmentsRecyclerView.post {
                businessAdapter.notifyDataSetChanged()
            }
        } else {
            Log.e("ClienteHomeFragment", "❌ businessAdapter não inicializado!")
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

    // 🎨 FUNÇÃO: Controlar exibição de estabelecimentos vazios
    private fun showNoEstablishmentsMessage(show: Boolean, customMessage: Boolean = false) {
        if (_binding == null || !isAdded) return

        binding.noEstablishmentsContainer.visibility = if (show) View.VISIBLE else View.GONE
        binding.establishmentsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        
        // 🔧 Só atualizar mensagem se não for customizada
        if (show && !customMessage) {
            binding.noEstablishmentsTitle.text = "Nenhum estabelecimento encontrado"
            val message = when {
                currentUserLat != null && currentUserLon != null -> {
                    "Não encontramos estabelecimentos em um raio de ${DEFAULT_RADIUS_KM.toInt()}km da sua localização."
                }
                currentCity != null -> {
                    "Não encontramos estabelecimentos em $currentCity."
                }
                else -> {
                    "Não encontramos estabelecimentos na sua região."
                }
            }
            binding.noEstablishmentsMessage.text = message
        }
    }


    // ❤️ GERENCIAMENTO DE FAVORITOS
    private fun handleFavoriteClick(business: Business, isFavorite: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        if (isFavorite) {
            // Adicionar aos favoritos
            addToFavorites(userId, business.ownerId)
            android.widget.Toast.makeText(
                requireContext(), 
                "${business.name} adicionado aos favoritos!", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            // Remover dos favoritos
            removeFromFavorites(userId, business.ownerId)
            android.widget.Toast.makeText(
                requireContext(), 
                "${business.name} removido dos favoritos", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun addToFavorites(userId: String, businessId: String) {
        firestore.collection("users").document(userId)
            .collection("favorites").document(businessId)
            .set(mapOf(
                "businessId" to businessId,
                "addedAt" to System.currentTimeMillis()
            ))
            .addOnSuccessListener {
                Log.d("ClienteHomeFragment", "✅ Favorito adicionado: $businessId")
                
                // 🔄 Atualizar lista local e reordenar
                userFavoriteIds.add(businessId)
                if (businessList.isNotEmpty()) {
                    updateBusinessAdapter(businessList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClienteHomeFragment", "❌ Erro ao adicionar favorito: ${e.message}")
            }
    }
    
    private fun removeFromFavorites(userId: String, businessId: String) {
        firestore.collection("users").document(userId)
            .collection("favorites").document(businessId)
            .delete()
            .addOnSuccessListener {
                Log.d("ClienteHomeFragment", "✅ Favorito removido: $businessId")
                
                // 🔄 Atualizar lista local e reordenar
                userFavoriteIds.remove(businessId)
                if (businessList.isNotEmpty()) {
                    updateBusinessAdapter(businessList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClienteHomeFragment", "❌ Erro ao remover favorito: ${e.message}")
            }
    }
    
    // ⭐ NOVA FUNÇÃO: Ordenar estabelecimentos com favoritos no topo
    private val userFavoriteIds = mutableSetOf<String>()
    
    private fun sortBusinessesByFavorites(businesses: List<Business>): List<Business> {
        return businesses.sortedWith(compareBy<Business> { business ->
            // Favoritos primeiro (false = 0, true = 1, então favoritos vêm primeiro)
            !userFavoriteIds.contains(business.ownerId)
        }.thenBy { business ->
            // Depois ordenar por distância se disponível
            if (currentUserLat != null && currentUserLon != null && 
                business.latitude != 0.0 && business.longitude != 0.0) {
                calculateDistance(currentUserLat!!, currentUserLon!!, business.latitude, business.longitude)
            } else {
                Double.MAX_VALUE
            }
        })
    }
    
    private fun loadUserFavorites() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        firestore.collection("users").document(userId)
            .collection("favorites")
            .get()
            .addOnSuccessListener { documents ->
                val favoriteIds = documents.map { it.id }.toSet()
                
                // 🎯 Atualizar conjunto local de favoritos
                userFavoriteIds.clear()
                userFavoriteIds.addAll(favoriteIds)
                
                if (::businessAdapter.isInitialized) {
                    businessAdapter.setFavoriteBusinesses(favoriteIds)
                    
                    // 🔄 Reordenar lista atual se houver estabelecimentos
                    if (businessList.isNotEmpty()) {
                        updateBusinessAdapter(businessList)
                    }
                }
                Log.d("ClienteHomeFragment", "✅ Carregados ${favoriteIds.size} favoritos")
            }
            .addOnFailureListener { e ->
                Log.e("ClienteHomeFragment", "❌ Erro ao carregar favoritos: ${e.message}")
                // 🔧 Se houver erro, usar cache local vazio
                userFavoriteIds.clear()
            }
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
