package com.conect.aplicativoconect.view.ui.client

import CategoriesPagerAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentClienteHomeBinding
import com.conect.aplicativoconect.view.TokenUtils
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.ui.admin.BusinessAdapter
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        setupAdapters()
        fetchBusinesses()  // Busca os estabelecimentos
        setupSearchView()

        // Mostra categorias e agendamentos ao iniciar
        showDefaultView()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            fetchUserBookings(it)
            clientViewModel.loadUserData(it)
        }

        observeUserData()
    }

    private fun setupAdapters() {
        val categories = listOf("Manicure", "Barbearia", "Cabeleireiro", "Massagista", "Maquiagens", "Estética")
        _binding?.categoriesRecyclerView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        _binding?.categoriesRecyclerView?.adapter = CategoriesPagerAdapter(categories) { selectedCategory ->
            filterBusinessesByCategory(selectedCategory)
        }

        businessAdapter = BusinessAdapter(requireContext(), businessList) { business ->
            fetchBusinessIdAndOpenDetails(business.name)
        }

        _binding?.establishmentsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        _binding?.establishmentsRecyclerView?.adapter = businessAdapter
    }

    private fun fetchUserBookings(userId: String) {
        if (_binding == null) return  // Verifica se o binding ainda está disponível

        binding.progressBar.visibility = View.VISIBLE  // Exibe o ProgressBar

        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val bookings = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Booking::class.java)?.apply { id = document.id }
                }.filter { isFutureBooking(it) }
                    .sortedBy { it.date }
                    .take(2)

                if (isAdded && _binding != null) {  // Verifica se o fragmento ainda está anexado
                    if (bookings.isEmpty()) {
                        showNoBookingsMessage(true)
                    } else {
                        showNoBookingsMessage(false)
                        setupClientBookingAdapter(bookings)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClienteHomeFragment", "Erro ao buscar agendamentos: ${e.message}")
                if (isAdded) {
                    Toast.makeText(requireContext(), "Erro ao buscar agendamentos.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnCompleteListener {
                if (_binding != null) {  // Verifique se o binding ainda existe
                    binding.progressBar.visibility = View.GONE  // Esconde o ProgressBar
                }
            }
    }

    private fun setupClientBookingAdapter(bookings: List<Booking>) {
        clientBookingAdapter = ClientBookingAdapter(
            bookings = bookings,
            onConfirmClick = { booking -> confirmBooking(booking) },
            onCancelClick = { booking -> cancelBooking(booking) }
        )
        _binding?.todayBookingsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        _binding?.todayBookingsRecyclerView?.adapter = clientBookingAdapter
    }

    private fun fetchBusinesses() {
        firestore.collection("business")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (isAdded && _binding != null) {  // Verifica se o fragmento ainda está anexado
                    if (!querySnapshot.isEmpty) {
                        businessList.clear()
                        businessList.addAll(querySnapshot.toObjects(Business::class.java))
                        updateBusinessAdapter(businessList)
                    } else {
                        Toast.makeText(requireContext(), "Nenhum estabelecimento encontrado.", Toast.LENGTH_SHORT).show()
                        fetchUserBookings(FirebaseAuth.getInstance().currentUser?.uid ?: "")  // Atualiza a lista
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ClienteHomeFragment", "Erro ao buscar estabelecimentos: ${e.message}")
                if (isAdded) {
                    Toast.makeText(requireContext(), "Erro ao buscar estabelecimentos.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun confirmBooking(booking: Booking) {
        firestore.collection("bookings").document(booking.id!!)
            .update("status_cliente", "confirmed")
            .addOnSuccessListener {
                sendNotificationToBusiness(
                    booking,
                    "Agendamento Confirmado",
                    "O agendamento de ${booking.name} foi confirmado!"
                )
                Toast.makeText(requireContext(), "Agendamento confirmado.", Toast.LENGTH_SHORT).show()
                fetchUserBookings(FirebaseAuth.getInstance().currentUser?.uid ?: "")  // Atualiza a lista
            }
            .addOnFailureListener { e ->
                Log.e("BookingFragment", "Erro ao confirmar agendamento: ${e.message}")
                Toast.makeText(requireContext(), "Erro ao confirmar agendamento.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun cancelBooking(booking: Booking) {
        firestore.collection("bookings").document(booking.id!!)
            .delete()
            .addOnSuccessListener {
                sendNotificationToBusiness(
                    booking,
                    "Agendamento Cancelado",
                    "O agendamento de ${booking.name} foi cancelado."
                )
                Toast.makeText(requireContext(), "Agendamento cancelado e excluído.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("BookingFragment", "Erro ao cancelar agendamento: ${e.message}")
                Toast.makeText(requireContext(), "Erro ao cancelar agendamento.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun sendNotificationToBusiness(booking: Booking, title: String, message: String) {
        val companyId = booking.companyId ?: return

        firestore.collection("business").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")
                Log.d("FCM", "Token FCM recebido: $fcmToken")  // Log para verificar o token

                if (!fcmToken.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        sendFCMNotification(fcmToken, title, message)
                    }
                } else {
                    Log.e("FCM", "Token FCM não encontrado para empresa: $companyId")
                }
            }
            .addOnFailureListener { e ->
                Log.e("BookingFragment", "Erro ao buscar token FCM: ${e.message}")
            }
    }

    // Exibe a visão padrão: categorias, agendamentos e estabelecimentos
    private fun showDefaultView() {
        _binding?.apply {
            todayAgendaCardView.visibility = View.VISIBLE  // Mostra agendamentos
            categoriesRecyclerView.visibility = View.VISIBLE  // Mostra categorias
            establishmentsRecyclerView.visibility = View.VISIBLE  // Mostra estabelecimentos
            categoriesTitle.visibility = View.VISIBLE  // Mostra o título "Categorias"
        }
    }

    private suspend fun sendFCMNotification(token: String, title: String, message: String) {
        val url = "https://fcm.googleapis.com/v1/projects/aplicativo-conect-f253d/messages:send"
        val payload = """
        {
          "message": {
            "token": "$token",
            "notification": {
              "title": "$title",
              "body": "$message"
            },
            "android": {
              "priority": "high"
            }
          }
        }
        """.trimIndent()

        val accessToken = withContext(Dispatchers.IO) {
            TokenUtils.getAccessTokenFromServiceAccount(requireContext())
        }

        if (accessToken == null) {
            Log.e("FCM", "Erro ao obter token de acesso.")
            return
        }

        val request = object : StringRequest(
            Method.POST, url,
            { response -> Log.d("FCM", "Notificação enviada: $response") },
            { error -> Log.e("FCM", "Erro ao enviar notificação: ${error.message}") }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/json"
                )
            }

            override fun getBody(): ByteArray = payload.toByteArray(Charsets.UTF_8)
        }

        withContext(Dispatchers.Main) {
            Volley.newRequestQueue(requireContext()).add(request)
        }
    }

    private fun observeUserData() {
        clientViewModel.userName.observe(viewLifecycleOwner) { userName ->
            _binding?.userName?.text = userName ?: "Nome do Usuário"
            updateGreeting()  // Atualiza a saudação com base no horário do dia
        }

        clientViewModel.userImage.observe(viewLifecycleOwner) { imageUrl ->
            _binding?.let { binding ->
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.foto_perfil_generica)
                    .error(R.drawable.foto_perfil_generica)
                    .into(binding.userImage)  // Define a imagem do usuário
            }
        }
    }

    private fun filterBusinessesByCategory(category: String) {
        val filteredBusinesses = businessList.filter { it.serviceType == category }
        if (filteredBusinesses.isEmpty()) {
            Toast.makeText(requireContext(), "Nenhum estabelecimento encontrado.", Toast.LENGTH_SHORT).show()
        }
        updateBusinessAdapter(filteredBusinesses)
    }

    // Configura a pesquisa e ajusta as visões dinamicamente
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

        // Restaura a visão original quando a pesquisa é limpa
        binding.searchView.setOnCloseListener {
            showDefaultView()
            false
        }
    }

    // Filtra os estabelecimentos e ajusta as visões
    private fun filterBusinesses(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            businessList  // Exibe todos os estabelecimentos se não houver pesquisa
        } else {
            businessList.filter { it.name.contains(query, ignoreCase = true) }
        }

        updateBusinessAdapter(filteredList)

        if (query.isNullOrEmpty()) {
            showDefaultView()  // Restaura a visão padrão se a pesquisa for limpa
        } else {
            showOnlyBusinesses()  // Mostra apenas estabelecimentos durante a pesquisa
        }
    }

    // Exibe apenas os estabelecimentos durante a pesquisa
    private fun showOnlyBusinesses() {
        _binding?.apply {
            todayAgendaCardView.visibility = View.GONE  // Esconde agendamentos
            categoriesRecyclerView.visibility = View.GONE  // Esconde categorias
            categoriesTitle.visibility = View.GONE  // Esconde o título "Categorias"
            establishmentsRecyclerView.visibility = View.VISIBLE  // Mostra estabelecimentos
        }
    }

    // Atualiza o adaptador de estabelecimentos
    private fun updateBusinessAdapter(filteredBusinesses: List<Business>) {
        if (isAdded && _binding != null) {
            businessAdapter = BusinessAdapter(requireContext(), filteredBusinesses) { business ->
                fetchBusinessIdAndOpenDetails(business.name)
            }
            _binding?.establishmentsRecyclerView?.adapter = businessAdapter
            businessAdapter.notifyDataSetChanged()
        } else {
            Log.e("ClienteHomeFragment", "Fragmento não está anexado. Não é possível atualizar o adapter.")
        }
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
        _binding?.apply {
            noBookingsMessage.visibility = if (show) View.VISIBLE else View.GONE
            noBookingsImage.visibility = if (show) View.VISIBLE else View.GONE
            todayBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        }
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
        _binding = null // Evita memory leaks
    }
}
