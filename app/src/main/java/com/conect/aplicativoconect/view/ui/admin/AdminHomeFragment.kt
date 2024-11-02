package com.conect.aplicativoconect.view.ui.admin

import BookingAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentAdminHomeBinding
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.ui.client.ClientBookingAdapter
import com.conect.aplicativoconect.view.viewmodel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore

    private val adminViewModel: AdminViewModel by activityViewModels()

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

        setupRecyclerView()
        loadBookingsInRealTime()
        loadBusinessName()

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

                firestore.collection("bookings")
                    .whereEqualTo("companyId", businessId)
                    .addSnapshotListener { querySnapshot, error ->
                        if (error != null) {
                            showErrorMessage("Erro ao carregar agendamentos.")
                            return@addSnapshotListener
                        }

                        val bookings = querySnapshot?.documents?.mapNotNull { document ->
                            document.toObject(Booking::class.java)?.apply { id = document.id }
                        } ?: emptyList()

                        processBookings(bookings)  // Processa e exibe os agendamentos
                    }
            }
            .addOnFailureListener { e ->
                showErrorMessage("Erro ao carregar dados da empresa.")
            }
    }

    private fun processBookings(bookings: List<Booking>) {
        // Filtra os agendamentos confirmados pelo cliente e pelo administrador
        val confirmedBookings = bookings.filter {
            it.status_cliente == "confirmed" && it.status_adm == "confirmed"
        }

        val (weeklyBookings, monthBookings) = filterBookingsByDate(confirmedBookings)
        val monthlyProfit = calculateProfit(monthBookings)
        val weeklyProfit = calculateProfit(weeklyBookings)

        if (!isAdded || _binding == null) return

        adminViewModel.setMonthBookingsCount(monthBookings.size)
        adminViewModel.setTodayBookingsCount(weeklyBookings.size)
        adminViewModel.setMonthProfit(monthlyProfit)
        adminViewModel.setTodayProfit(weeklyProfit)

        // Filtra e ordena para exibir os 2 agendamentos futuros mais próximos
        val nearestBookings = confirmedBookings
            .filter { isFutureBooking(it) }
            .sortedBy { it.date?.let { date -> parseDateTime(date, it.hour) } }
            .take(2)

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

        // Configura o intervalo do mês atual
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time

        calendar.add(Calendar.MONTH, 1) // Avança para o próximo mês
        calendar.set(Calendar.DAY_OF_MONTH, 1) // Define como o primeiro dia do próximo mês
        calendar.add(
            Calendar.MILLISECOND,
            -1
        ) // Volta um milissegundo para obter o último dia do mês atual
        val endOfMonth = calendar.time

        // Configura o intervalo da semana atual
        calendar.time = Date() // Define a data como hoje
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startOfWeek = calendar.time
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endOfWeek = calendar.time

        // Filtra os agendamentos semanais e mensais confirmados
        val weeklyBookings = bookings.filter { booking ->
            val bookingDate = booking.date?.let { parseDateTime(it, booking.hour) }
            bookingDate != null && bookingDate in startOfWeek..endOfWeek
        }

        val monthBookings = bookings.filter { booking ->
            val bookingDate = booking.date?.let { parseDateTime(it, booking.hour) }
            bookingDate != null && bookingDate in startOfMonth..endOfMonth
        }

        return Pair(weeklyBookings, monthBookings)
    }


    private fun setupNearestBookingsAdapter(nearestBookings: List<Booking>) {
        binding.todayBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.todayBookingsRecyclerView.adapter = ClientBookingAdapter(
            nearestBookings,
            onConfirmClick = { bookingId -> confirmBooking(bookingId.toString()) },
            onCancelClick = { bookingId -> cancelBooking(bookingId.toString()) },
            onEmptyList = { showNoBookingsMessage(true) } // Callback para lista vazia
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

    private fun showNoBookingsMessage(show: Boolean) {
        if (_binding != null) { // Verifica se o binding ainda está disponível
            binding.noBookingsMessage.visibility = if (show) View.VISIBLE else View.GONE
            binding.noBookingsImage.visibility = if (show) View.VISIBLE else View.GONE
            binding.todayBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        }
    }


    private fun showErrorMessage(message: String) {
        if (_binding != null) { // Verifica se o binding ainda está disponível
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
    }
}
