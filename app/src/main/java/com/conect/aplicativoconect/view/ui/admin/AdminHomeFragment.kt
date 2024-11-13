package com.conect.aplicativoconect.view.ui.admin

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
import com.conect.aplicativoconect.databinding.FragmentAdminHomeBinding
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.viewmodel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
        setupWeeklyDateRange()
        setupRecyclerView()
        loadBookingsInRealTime()
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

    private fun loadBookingsInRealTime() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("business")
            .whereEqualTo("ownerId", currentUserUid)
            .get()
            .addOnSuccessListener { businessSnapshot ->
                val businessId =
                    businessSnapshot.documents.firstOrNull()?.id ?: return@addOnSuccessListener

                // Atribui o listener à variável `bookingListener` para podermos removê-lo depois
                bookingListener = firestore.collection("bookings")
                    .whereEqualTo("companyId", businessId)
                    .addSnapshotListener { querySnapshot, error ->
                        if (!isAdded || _binding == null) return@addSnapshotListener  // Verifica se o fragmento ainda está anexado

                        if (error != null) {
                            showErrorMessage("Erro ao carregar agendamentos.")
                            return@addSnapshotListener
                        }

                        val bookings = querySnapshot?.documents?.mapNotNull { document ->
                            document.toObject(Booking::class.java)?.apply { id = document.id }
                        } ?: emptyList()

                        processBookings(bookings)
                    }
            }
            .addOnFailureListener { e ->
                if (isAdded && _binding != null) { // Verifica se o fragmento ainda está anexado
                    showErrorMessage("Erro ao carregar dados da empresa.")
                }
            }
    }

    private fun setupWeeklyDateRange() {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY  // Define domingo como primeiro dia da semana
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // Define o início da semana
        }
        startOfWeek = calendar.time

        calendar.add(Calendar.DAY_OF_WEEK, 6) // Move para o último dia da semana (sábado)
        endOfWeek = calendar.time
    }


    private fun processBookings(bookings: List<Booking>) {
        // Remova o filtro que exige que o status_adm seja "confirmed"
        val futureBookings = bookings.filter { isFutureBooking(it) }

        // Filtra agendamentos para a semana e o mês atual
        val (weeklyBookings, monthBookings) = filterBookingsByDate(futureBookings)
        val monthlyProfit = calculateProfit(monthBookings)
        val weeklyProfit = calculateProfit(weeklyBookings)

        if (!isAdded || _binding == null) return

        adminViewModel.setMonthBookingsCount(monthBookings.size)
        adminViewModel.setTodayBookingsCount(weeklyBookings.size)
        adminViewModel.setMonthProfit(monthlyProfit)
        adminViewModel.setTodayProfit(weeklyProfit)

        // Ajuste a lógica para mostrar todos os próximos agendamentos, não apenas os confirmados
        val nearestBookings = futureBookings
            .sortedBy { it.date?.let { date -> parseDateTime(date, it.hour) } }
            .take(2) // Exibe os 2 próximos agendamentos

        if (nearestBookings.isEmpty()) {
            showNoBookingsMessage(true)
        } else {
            showNoBookingsMessage(false)
            setupNearestBookingsAdapter(nearestBookings)
        }
    }


    private fun calculateProfit(bookings: List<Booking>): Double {
        return bookings.sumOf { it.price }
    }

    private fun filterBookingsByDate(bookings: List<Booking>): Pair<List<Booking>, List<Booking>> {
        val calendar = Calendar.getInstance()

        // Configura o primeiro e último dia do mês
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.time

        // Filtra agendamentos para a semana atual, independente do status_adm
        val weeklyBookings = bookings.filter { booking ->
            val bookingDate = booking.date?.let { parseDateTime(it, booking.hour) }
            bookingDate != null && bookingDate in (startOfWeek ?: Date())..(endOfWeek ?: Date())
        }

        // Filtra agendamentos do mês que são confirmados pelo cliente
        val monthBookings = bookings.filter { booking ->
            val bookingDate = booking.date?.let { parseDateTime(it, booking.hour) }
            bookingDate != null && bookingDate in startOfMonth..endOfMonth && booking.status_cliente == "confirmed"
        }

        return Pair(weeklyBookings, monthBookings)
    }


    private fun setupNearestBookingsAdapter(nearestBookings: List<Booking>) {
        binding.todayBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.todayBookingsRecyclerView.adapter = BookingAdapter(
            bookings = nearestBookings,
            context = requireContext(),
            onConfirmBooking = { bookingId -> confirmBooking(bookingId) },
            onCancelBooking = { bookingId -> cancelBooking(bookingId) }
        )
    }

    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDateTime = Calendar.getInstance().time
        val bookingDateTime = booking.date?.let { parseDateTime(it, booking.hour) }
        return bookingDateTime?.after(currentDateTime) ?: false
    }


    private fun parseDateTime(date: String, hour: String): Date? {
        return try {
            val dateParts = date.split("/").map { it.toInt() }
            val timeParts = hour.split(":").map { it.toIntOrNull() ?: 0 }

            Calendar.getInstance().apply {
                set(Calendar.YEAR, dateParts[2])
                set(Calendar.MONTH, dateParts[1] - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[0])
                set(Calendar.HOUR_OF_DAY, timeParts[0])
                set(Calendar.MINUTE, timeParts.getOrElse(1) { 0 })
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        } catch (e: Exception) {
            Log.e("AdminHomeFragment", "Erro ao analisar a data/hora: ${e.message}")
            null
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
        binding.tipsViewPager.adapter = tipsAdapter
        binding.tipsViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
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
                loadBookingsInRealTime()
            }
            .addOnFailureListener { e ->
                Log.w("AdminHomeFragment", "Erro ao confirmar o agendamento: ", e)
            }
    }

    private fun cancelBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId).delete()
            .addOnSuccessListener {
                Log.d("CancelBooking", "Agendamento cancelado com sucesso!")
                loadBookingsInRealTime()
            }
            .addOnFailureListener { e ->
                Log.w("CancelBooking", "Erro ao cancelar agendamento", e)
            }
    }

    private fun loadBusinessName() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            binding.userNameBusiness.text = "Usuário não autenticado"
            setGreeting("Usuário") // Saudação mesmo que o usuário não esteja autenticado
            return
        }

        firestore.collection("business")
            .whereEqualTo("ownerId", currentUserUid)
            .get()
            .addOnSuccessListener { documents ->
                val business = documents.firstOrNull()?.toObject(Business::class.java)
                val businessName = business?.name ?: "Nome não disponível"

                binding.userNameBusiness.text = businessName  // Exibe o nome diretamente
                setGreeting(businessName)  // Chama a saudação com o nome do negócio
                loadProfileImage(business?.imageUrl)
            }
            .addOnFailureListener { e ->
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
