package com.conect.aplicativoconect.view.ui.admin

import BookingAdapter
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
import com.conect.aplicativoconect.view.viewmodel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

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

        // Observa mudanças nos ViewModels
        adminViewModel.todayBookingsCount.observe(viewLifecycleOwner) { count ->
            binding.weekBookingsCount.text = count.toString()
        }

        adminViewModel.monthBookingsCount.observe(viewLifecycleOwner) { count ->
            binding.monthBookingsCount.text = count.toString()
        }

        adminViewModel.todayProfit.observe(viewLifecycleOwner) { profit ->
            binding.todayProfitTextView.text = String.format("R$ %.2f", profit)
        }

        adminViewModel.monthProfit.observe(viewLifecycleOwner) { profit ->
            binding.monthProfitTextView.text = String.format("R$ %.2f", profit)
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
                val businessId = businessSnapshot.documents.firstOrNull()?.id ?: return@addOnSuccessListener

                firestore.collection("bookings")
                    .whereEqualTo("companyId", businessId)
                    .addSnapshotListener { querySnapshot, error ->
                        if (error != null) {
                            Log.e("AdminHomeFragment", "Erro ao buscar agendamentos: ${error.message}")
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
                Log.e("AdminHomeFragment", "Erro ao buscar empresa: ${e.message}")
                showErrorMessage("Erro ao carregar dados da empresa.")
            }
    }

    private fun processBookings(bookings: List<Booking>) {
        val confirmedBookings = bookings.filter { it.status_cliente == "confirmed" }

        val (weeklyBookings, monthBookings) = filterBookingsByDate(confirmedBookings)

        val weeklyProfit = calculateProfit(weeklyBookings)
        val monthlyProfit = calculateProfit(monthBookings)

        Log.d("AdminHomeFragment", "Agendamentos confirmados: ${confirmedBookings.size}")
        Log.d("AdminHomeFragment", "Lucro da semana: R$ $weeklyProfit")
        Log.d("AdminHomeFragment", "Lucro do mês: R$ $monthlyProfit")

        if (!isAdded || _binding == null) return

        adminViewModel.setTodayBookingsCount(weeklyBookings.size)
        adminViewModel.setMonthBookingsCount(monthBookings.size)
        adminViewModel.setTodayProfit(weeklyProfit)
        adminViewModel.setMonthProfit(monthlyProfit)

        val nearestBookings = bookings
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

        // Definindo o início do mês atual
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time

        // Definindo o fim do mês atual
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.time

        Log.d("AdminHomeFragment", "Filtro Mensal - Início: $startOfMonth, Fim: $endOfMonth")

        // Filtro para os agendamentos da semana
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val startOfWeek = calendar.time

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endOfWeek = calendar.time

        val weeklyBookings = bookings.filter { booking ->
            val bookingDate = booking.date?.let { date -> parseDateTime(date, booking.hour).time }
            Log.d("AdminHomeFragment", "Verificando agendamento semanal: $bookingDate no intervalo $startOfWeek até $endOfWeek")
            bookingDate != null && bookingDate in startOfWeek..endOfWeek
        }

        val monthBookings = bookings.filter { booking ->
            val bookingDate = booking.date?.let { date -> parseDateTime(date, booking.hour).time }
            Log.d("AdminHomeFragment", "Verificando agendamento mensal: $bookingDate no intervalo $startOfMonth até $endOfMonth")
            bookingDate != null && bookingDate in startOfMonth..endOfMonth
        }

        Log.d("AdminHomeFragment", "Total de agendamentos do mês: ${monthBookings.size}")
        return Pair(weeklyBookings, monthBookings)
    }

    private suspend fun getAllBookings(businessId: String): List<Booking> {
        val businessSnapshot = firestore.collection("business").document(businessId).get().await()
        val business = businessSnapshot.toObject(Business::class.java)

        val services = business?.services ?: emptyList()
        Log.d("AdminHomeFragment", "Total de serviços carregados: ${services.size}")

        val bookingsSnapshot = firestore.collection("bookings")
            .whereEqualTo("companyId", businessId)
            .get()
            .await()

        return bookingsSnapshot.documents.mapNotNull { document ->
            val booking = document.toObject(Booking::class.java)
            booking?.id = document.id

            Log.d(
                "AdminHomeFragment",
                "Agendamento ID: ${booking?.id}, Serviço: ${booking?.serviceName}, Preço: ${booking?.price}"
            )

            // Buscar serviço correspondente ignorando espaços e diferenças de case
            val matchedService = services.firstOrNull { service ->
                service.name.trim().equals(booking?.serviceName?.trim(), ignoreCase = true)
            }

            if (matchedService != null) {
                booking?.price = matchedService.price
                Log.d(
                    "AdminHomeFragment",
                    "Serviço encontrado: ${matchedService.name}, Preço: ${matchedService.price}"
                )
            } else {
                Log.d(
                    "AdminHomeFragment",
                    "Serviço não encontrado para o agendamento: ${booking?.serviceName}"
                )
            }

            booking
        }
    }

    private fun setupNearestBookingsAdapter(nearestBookings: List<Booking>) {
        binding.todayBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.todayBookingsRecyclerView.adapter = BookingAdapter(
            nearestBookings,
            requireContext(),
            { bookingId -> confirmBooking(bookingId) },
            { bookingId -> cancelBooking(bookingId) }
        )
    }

    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDateTime = Calendar.getInstance()
        val bookingDateTime = booking.date?.let { parseDateTime(it, booking.hour) }
        return bookingDateTime?.after(currentDateTime) ?: false
    }


    private fun parseDateTime(date: String, hour: Int?): Calendar {
        val dateParts = date.split("/").map { it.toInt() }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, dateParts[2])
            set(Calendar.MONTH, dateParts[1] - 1) // Mês é indexado em 0
            set(Calendar.DAY_OF_MONTH, dateParts[0])
            set(Calendar.HOUR_OF_DAY, hour ?: 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun showNoBookingsMessage(show: Boolean) {
        binding.noBookingsMessage.visibility = if (show) View.VISIBLE else View.GONE
        binding.noBookingsImage.visibility = if (show) View.VISIBLE else View.GONE
        binding.todayBookingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showErrorMessage(message: String) {
        binding.noBookingsMessage.text = message
        binding.noBookingsMessage.visibility = View.VISIBLE
        binding.noBookingsImage.visibility = View.VISIBLE
        binding.todayBookingsRecyclerView.visibility = View.GONE
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
                Log.e("AdminHomeFragment", "Erro ao buscar nome do negócio: ", e)
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
