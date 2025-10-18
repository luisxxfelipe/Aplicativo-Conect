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
import com.conect.aplicativoconect.utils.AuthHelper
import com.conect.aplicativoconect.utils.ImageHelper
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
    private lateinit var calendarDayAdapter: com.conect.aplicativoconect.ui.adapters.CalendarDayAdapter
    private var calendarDays: List<Date> = emptyList()
    private var selectedCalendarDate: Date? = null

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
            binding.todayBookingsCount.text = count.toString()
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
        setupCalendarDaysRecycler()

        // Clique no título Agendamentos para abrir DatePicker
        binding.tvAgendamentos.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = android.app.DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                val selectedDateMillis = selectedCalendar.timeInMillis
                filterBookingsByDate(selectedDateMillis)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }
    }
    // Filtra agendamentos pela data selecionada
    private fun filterBookingsByDate(dateMillis: Long) {
        val currentUserUid = AuthHelper.getCurrentUserId() ?: return
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        firestore.collection("bookings")
            .whereEqualTo("companyId", currentUserUid)
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .whereLessThanOrEqualTo("timestamp", endOfDay)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val bookings = snapshot.documents.mapNotNull { document ->
                    document.toObject(Booking::class.java)?.apply { id = document.id }
                }
                if (bookings.isEmpty()) {
                    showNoBookingsMessage(true)
                    binding.noBookingsMessage.text = "Nenhum agendamento para esta data."
                } else {
                    showNoBookingsMessage(false)
                    setupNearestBookingsAdapter(bookings)
                }
            }
            .addOnFailureListener {
                showErrorMessage("Erro ao buscar agendamentos da data.")
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
        val currentUserUid = AuthHelper.getCurrentUserId() ?: return // ✅ OTIMIZADO: Helper centralizado

        // Carrega TODOS os agendamentos para cálculo de lucros (passados + futuros)
        bookingListener = firestore.collection("bookings")
            .whereEqualTo("companyId", currentUserUid)
            .orderBy("timestamp", Query.Direction.DESCENDING) // Mais recentes primeiro
            .limit(100) // Aumentar limite para incluir histórico
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
                    // Mesmo sem agendamentos, calcula lucros (serão zero)
                    calculateAndUpdateProfits(emptyList())
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

        // Calcula lucros baseado em agendamentos CONCLUÍDOS (do passado)
        val completedBookings = bookings.filter { booking ->
            (booking.timestamp ?: 0) < System.currentTimeMillis() &&
            (booking.status_adm == "confirmed" || booking.status_adm == "completed")
        }

        // Calcula e atualiza lucros
        calculateAndUpdateProfits(completedBookings)

        // Filtra apenas agendamentos FUTUROS para exibição
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

    private fun calculateAndUpdateProfits(completedBookings: List<Booking>) {
        val currentTime = System.currentTimeMillis()

        // Obtém o timestamp do início de hoje
        val calendarTodayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfTodayMillis = calendarTodayStart.timeInMillis

        // Obtém o timestamp do início do mês
        val calendarMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonthMillis = calendarMonthStart.timeInMillis

        // Filtro para agendamentos concluídos de hoje
        val todayCompletedBookings = completedBookings.filter { booking ->
            booking.timestamp!! >= startOfTodayMillis && booking.timestamp!! < currentTime
        }

        // Filtro para agendamentos concluídos do mês
        val monthCompletedBookings = completedBookings.filter { booking ->
            booking.timestamp!! >= startOfMonthMillis && booking.timestamp!! < currentTime
        }

        // Conta agendamentos (todos, independente do preço)
        val todayTotalBookings = completedBookings.count { booking ->
            booking.timestamp!! >= startOfTodayMillis && booking.timestamp!! < currentTime
        }

        val monthTotalBookings = completedBookings.count { booking ->
            booking.timestamp!! >= startOfMonthMillis && booking.timestamp!! < currentTime
        }

        // Calcula lucros (só preços positivos)
        val todayProfit = completedBookings
            .filter { booking -> booking.timestamp!! >= startOfTodayMillis && booking.timestamp!! < currentTime }
            .sumOf { booking -> booking.price?.takeIf { it > 0.0 } ?: 0.0 }

        val monthProfit = completedBookings
            .filter { booking -> booking.timestamp!! >= startOfMonthMillis && booking.timestamp!! < currentTime }
            .sumOf { booking -> booking.price?.takeIf { it > 0.0 } ?: 0.0 }

        // Atualiza os valores no ViewModel
        adminViewModel.setTodayBookingsCount(todayTotalBookings)
        adminViewModel.setMonthBookingsCount(monthTotalBookings)
        adminViewModel.setTodayProfit(todayProfit)
        adminViewModel.setMonthProfit(monthProfit)
    }


    private fun isFutureBooking(booking: Booking): Boolean {
        val currentTimestamp = System.currentTimeMillis()
        return booking.timestamp!! > currentTimestamp
    }

    private fun calculateProfit(bookings: List<Booking>): Double {
        return bookings.sumOf { booking ->
            booking.price ?: 0.0 // Se price for null, usa 0.0
        }
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
            binding.noBookingsContainer.visibility = if (show) View.VISIBLE else View.GONE
            binding.todayBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun showErrorMessage(message: String) {
        if (isAdded && _binding != null) {  // Verifica se o binding ainda está disponível
            binding.noBookingsMessage.text = message
            binding.noBookingsContainer.visibility = View.VISIBLE
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
        val currentUserUid = AuthHelper.getCurrentUserId()
        if (currentUserUid == null) {
            Log.e("AdminHomeFragment", "Usuário não autenticado.")
            binding.userNameBusiness.text = "Usuário não autenticado"
            setGreeting("Usuário") // Saudação mesmo que o usuário não esteja autenticado
            return
        } // ✅ OTIMIZADO: Helper centralizado

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
                
                // Verificar se o binding ainda está disponível
                _binding?.let { binding ->
                    binding.userNameBusiness.text = businessName // Exibe o nome diretamente
                    setGreeting(businessName) // Chama a saudação com o nome do negócio
                    loadProfileImage(business?.imageUrl)
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminHomeFragment", "Erro ao carregar os dados da empresa: ${e.message}", e)
                // Verificar se o binding ainda está disponível
                _binding?.let { binding ->
                    binding.userNameBusiness.text = "Erro ao carregar nome"
                    setGreeting("Usuário")
                }
            }

    }

    private fun loadProfileImage(imageUrl: String?) {
        // Verificar se o binding ainda está disponível
        _binding?.let { binding ->
            ImageHelper.loadProfileImage(requireContext(), imageUrl, binding.userImage) // ✅ OTIMIZADO: Helper centralizado
        }
    }

    // Configura o RecyclerView horizontal de datas do mês
    private fun setupCalendarDaysRecycler() {
        val calendar = Calendar.getInstance()

        // Começar do dia atual em vez do dia 1
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val daysList = mutableListOf<Date>()

        // Adicionar dias do mês atual a partir do dia atual
        for (day in currentDay..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            daysList.add(calendar.time)
        }

        // Se quiser mostrar também os próximos dias do próximo mês, descomente:
        // val nextMonthDays = 7 - daysList.size // Mostrar até completar uma semana
        // if (nextMonthDays > 0) {
        //     calendar.add(Calendar.MONTH, 1)
        //     calendar.set(Calendar.DAY_OF_MONTH, 1)
        //     for (day in 1..nextMonthDays) {
        //         calendar.set(Calendar.DAY_OF_MONTH, day)
        //         daysList.add(calendar.time)
        //     }
        // }

        calendarDays = daysList

        // Sempre seleciona o dia atual por padrão
        val today = Calendar.getInstance().time
        selectedCalendarDate = today

        calendarDayAdapter = com.conect.aplicativoconect.ui.adapters.CalendarDayAdapter(
            days = calendarDays,
            selectedDate = selectedCalendarDate,
            onDayClick = { date ->
                if (date == null) {
                    // Deselecionado: mostrar agendamentos padrão (mais próximos)
                    selectedCalendarDate = today // Volta para hoje
                    loadBookings()
                } else {
                    selectedCalendarDate = date
                    filterBookingsByDate(date.time)
                }
            }
        )
        binding.rvCalendarDays.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = calendarDayAdapter
        }

        // Força atualização inicial para mostrar seleção
        calendarDayAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Remove o listener para evitar leaks e erros quando o fragmento for destruído
        bookingListener?.remove()
        handler.removeCallbacks(runnable)
    }
}
