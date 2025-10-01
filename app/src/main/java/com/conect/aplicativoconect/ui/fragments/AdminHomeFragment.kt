package com.conect.aplicativoconect.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Booking
import com.conect.aplicativoconect.data.models.Business
import com.conect.aplicativoconect.databinding.FragmentAdminHomeBinding
import com.conect.aplicativoconect.ui.activities.AddBookingActivity
import com.conect.aplicativoconect.ui.adapters.BookingAdapter
import com.conect.aplicativoconect.ui.adapters.TipsAdapter
import com.conect.aplicativoconect.ui.viewmodels.AdminViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private var bookingListener: ListenerRegistration? = null
    private val adminViewModel: AdminViewModel by activityViewModels()
    private lateinit var tipsAdapter: TipsAdapter
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var currentTipIndex = 0
    private var startOfWeek: Date? = null
    private var endOfWeek: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        setupWeeklyDateRange()
        setupRecyclerView()
        loadBookings()
        loadBusinessName()

        setupTipsViewPager()
        startTipRotation()

        // Configurando o clique do FAB
        binding.fabAddBooking.setOnClickListener {
            // Aqui você inicia a Activity de agendamento
            val intent = Intent(
                requireContext(),
                AddBookingActivity::class.java
            ) // Substitua pelo nome correto da sua Activity
            startActivity(intent)
        }

        // Observa mudanças nos ViewModels
        adminViewModel.todayBookingsCount.observe(viewLifecycleOwner) { count ->
            binding.weekBookingsCount.text = count.toString()
        }

        adminViewModel.monthBookingsCount.observe(viewLifecycleOwner) { count ->
            binding.monthBookingsCount.text = count.toString()
        }

        adminViewModel.todayProfit.observe(viewLifecycleOwner) { profit ->
            binding.todayProfitTextView.text = String.format(Locale.getDefault(), "R$ %.2f", profit)
        }

        adminViewModel.monthProfit.observe(viewLifecycleOwner) { profit ->
            binding.monthProfitTextView.text = String.format(Locale.getDefault(), "R$ %.2f", profit)
        }

    }

    private fun setGreeting(name: String) {
        val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hourOfDay < 6 -> "Boa madrugada!"
            hourOfDay < 12 -> "Bom dia!"
            hourOfDay < 18 -> "Boa tarde!"
            else -> "Boa noite!"
        }
        binding.greetingTextView.text = greeting
    }

    private fun setupRecyclerView() {
        binding.todayBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun loadBookings() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Use um listener em tempo real
        bookingListener = firestore.collection("bookings")
            .whereEqualTo("companyId", currentUserUid)
            .whereGreaterThanOrEqualTo("timestamp", System.currentTimeMillis())
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminHomeFragment", "Erro ao carregar agendamentos: ${error.message}")
                    showErrorMessage("Erro ao carregar agendamentos.")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val allBookings = snapshot.documents.mapNotNull { document ->
                        document.toObject(Booking::class.java)?.apply { id = document.id }
                    }
                    processBookings(allBookings)
                } else {
                    showNoBookingsMessage(true)
                }
            }
    }

    private fun setupWeeklyDateRange() {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // Início da semana
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        startOfWeek = calendar.time

        calendar.add(Calendar.DAY_OF_WEEK, 6) // Final da semana (sábado)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        endOfWeek = calendar.time
    }

    private fun processBookings(bookings: List<Booking>) {
        if (!isAdded || _binding == null) return

        // Obtém o timestamp do início e fim da semana
        val startOfWeekMillis = startOfWeek?.time ?: 0
        val endOfWeekMillis = endOfWeek?.time ?: Long.MAX_VALUE

        // Obtém o timestamp do início e fim do mês
        val startOfMonthMillis = startOfMonth().time
        val endOfMonthMillis = endOfMonth().time

        // Filtro para agendamentos dentro da semana atual
        val weeklyBookings = bookings.filter { booking ->
            booking.timestamp in startOfWeekMillis..endOfWeekMillis
        }

        // Filtro para agendamentos dentro do mês atual
        val monthBookings = bookings.filter { booking ->
            booking.timestamp in startOfMonthMillis..endOfMonthMillis
        }

        // Calcula os lucros semanais e mensais
        val weeklyProfit = calculateProfit(weeklyBookings)
        val monthlyProfit = calculateProfit(monthBookings)

        // Atualiza os valores no ViewModel
        adminViewModel.setTodayBookingsCount(weeklyBookings.size) // Total semanal
        adminViewModel.setMonthBookingsCount(monthBookings.size) // Total mensal
        adminViewModel.setTodayProfit(weeklyProfit)
        adminViewModel.setMonthProfit(monthlyProfit)

        // Filtra os agendamentos futuros
        val futureBookings = bookings.filter { isFutureBooking(it) }

        // Ordena e seleciona os 2 agendamentos mais próximos
        val nearestBookings = futureBookings.sortedBy { it.timestamp }.take(2)

        if (nearestBookings.isEmpty()) {
            showNoBookingsMessage(true)
        } else {
            showNoBookingsMessage(false)
            setupNearestBookingsAdapter(nearestBookings)
        }
    }


    private fun isFutureBooking(booking: Booking): Boolean {
        val currentTimestamp = System.currentTimeMillis()
        return booking.timestamp!! > currentTimestamp
    }

    private fun startOfMonth(): Date {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    private fun endOfMonth(): Date {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1) // Último instante do mês
        }
        return calendar.time
    }

    private fun calculateProfit(bookings: List<Booking>): Double {
        return bookings.sumOf { it.price }
    }

    private fun setupNearestBookingsAdapter(nearestBookings: List<Booking>) {
        binding.todayBookingsRecyclerView.apply {
            setHasFixedSize(true) // Melhora a performance se o tamanho não muda
            layoutManager = LinearLayoutManager(context)
            adapter = BookingAdapter(
                bookings = nearestBookings,
                context = requireContext(),
                onConfirmBooking = { bookingId -> confirmBooking(bookingId) },
                onCancelBooking = { bookingId -> cancelBooking(bookingId) }
            )
        }
    }

    private fun setupTipsViewPager() {
        val tips = listOf(
            "Mantenha os clientes por perto! Use nosso sistema de agendamento para garantir que eles sempre voltem.",
            "Aproveite os dados ao seu alcance: veja quais serviços estão em alta e ofereça promoções que seus clientes vão amar!",
            "Comunicação é chave! Use as notificações para lembrar clientes de horários e promoções exclusivas.",
            "Crescimento começa com planejamento! Use o app para acompanhar lucros e identifique as melhores semanas para expandir seus serviços.",
            "Ofereça avaliações e veja seu negócio crescer! Peça feedback e entenda o que seus clientes mais valorizam.",
            "Gerencie seu negócio de qualquer lugar! Com nosso app, seus agendamentos e lucros estão sempre à sua mão."
        )
        tipsAdapter = TipsAdapter(tips)
        binding.tipsViewPager.apply {
            adapter = tipsAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            offscreenPageLimit = 1 // Limita páginas pré-carregadas
        }
    }


    private fun startTipRotation() {
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                if (currentTipIndex < tipsAdapter.itemCount - 1) {
                    currentTipIndex++
                } else {
                    currentTipIndex = 0
                }
                binding.tipsViewPager.setCurrentItem(currentTipIndex, true)
                handler.postDelayed(this, 10000) // 10 segundos
            }
        }
        handler.postDelayed(runnable, 20000)
    }

    private fun showNoBookingsMessage(show: Boolean) {
        if (isAdded && _binding != null) {  // Verifica se o binding ainda está disponível
            binding.noBookingsMessage.visibility = if (show) View.VISIBLE else View.GONE
            binding.noBookingsImage.visibility = if (show) View.VISIBLE else View.GONE
            binding.todayBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun showErrorMessage(message: String) {
        if (isAdded && _binding != null) {  // Verifica se o binding ainda está disponível
            binding.noBookingsMessage.text = message
            binding.noBookingsMessage.visibility = View.VISIBLE
            binding.noBookingsImage.visibility = View.VISIBLE
            binding.todayBookingsRecyclerView.visibility = View.GONE
        }
    }

    private fun confirmBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .update("status_adm", "confirmed")
            .addOnSuccessListener {
                Log.d("AdminHomeFragment", "Agendamento confirmado com sucesso!")
                loadBookings()
            }
            .addOnFailureListener { e ->
                Log.w("AdminHomeFragment", "Erro ao confirmar o agendamento: ", e)
            }
    }

    private fun cancelBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId).delete()
            .addOnSuccessListener {
                Log.d("CancelBooking", "Agendamento cancelado com sucesso!")
                loadBookings()
            }
            .addOnFailureListener { e ->
                Log.w("CancelBooking", "Erro ao cancelar agendamento", e)
            }
    }

    private fun loadBusinessName() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.e("AdminHomeFragment", "Usuário não autenticado.")
            binding.userNameBusiness.text = "Usuário não autenticado"
            setGreeting("Usuário") // Saudação mesmo que o usuário não esteja autenticado
            return
        }

        Log.d("AdminHomeFragment", "Usuário autenticado com UID: $currentUserUid")

        firestore.collection("business")
            .document(currentUserUid) // Acessa diretamente o documento com o UID como ID
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    setGreeting("Usuário")
                    return@addOnSuccessListener
                }

                val business = document.toObject(Business::class.java)
                val businessName = business?.name ?: "Nome não disponível"

                Log.d("AdminHomeFragment", "Empresa encontrada: $businessName")
                binding.userNameBusiness.text = businessName // Exibe o nome diretamente
                setGreeting(businessName) // Chama a saudação com o nome do negócio
                loadProfileImage(business?.imageUrl)
            }
            .addOnFailureListener { e ->
                Log.e("AdminHomeFragment", "Erro ao carregar os dados da empresa: ${e.message}", e)
                binding.userNameBusiness.text = "Erro ao carregar nome"
                setGreeting("Usuário")
            }

    }

    private fun loadProfileImage(imageUrl: String?) {
        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.foto_perfil_generica)
                .error(R.drawable.foto_perfil_generica)
                .into(binding.userImage)
        } ?: binding.userImage.setImageResource(R.drawable.foto_perfil_generica)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Remove o listener para evitar leaks e erros quando o fragmento for destruído
        bookingListener?.remove()
        handler.removeCallbacks(runnable)
    }
}
