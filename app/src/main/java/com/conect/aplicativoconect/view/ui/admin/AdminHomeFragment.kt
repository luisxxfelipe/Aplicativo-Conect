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
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.data.repository.BookingRepository
import com.conect.aplicativoconect.view.viewmodel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!

    private val bookingRepository = BookingRepository()
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
        loadDataSafely()
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

        adminViewModel.businessName.observe(viewLifecycleOwner) { name ->
            binding.userNameBusiness.text = name ?: "Nome não disponível"
            setGreeting(name ?: "Usuário")
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

    private fun loadDataSafely() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                val businessSnapshot = firestore.collection("business")
                    .whereEqualTo("ownerId", currentUserUid)
                    .get()
                    .await()

                val businessId = businessSnapshot.documents.firstOrNull()?.id ?: return@launch

                val weeklyBookings = bookingRepository.getWeeklyBookingsByCompany(businessId)
                val monthBookings = bookingRepository.getMonthBookingsByCompany(businessId)

                val weeklyProfit = bookingRepository.getWeeklyProfitByCompany(businessId)
                val monthlyProfit = bookingRepository.getMonthProfitByCompany(businessId)

                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext  // Verifica se o fragmento ainda está ativo

                    adminViewModel.setTodayBookingsCount(weeklyBookings.size)
                    adminViewModel.setMonthBookingsCount(monthBookings.size)
                    adminViewModel.setTodayProfit(weeklyProfit)
                    adminViewModel.setMonthProfit(monthlyProfit)

                    if (weeklyBookings.isEmpty()) {
                        showNoBookingsMessage(true)
                    } else {
                        showNoBookingsMessage(false)
                        binding.todayBookingsRecyclerView.adapter = BookingAdapter(
                            weeklyBookings,
                            requireContext(),
                            { bookingId -> confirmBooking(bookingId) },
                            { bookingId -> cancelBooking(bookingId) }
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext  // Verifica se o fragmento ainda está ativo
                    showErrorMessage("Erro ao carregar dados.")
                    Log.e("AdminHomeFragment", "Erro ao carregar dados: ", e)
                }
            }
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
            .update("status", "confirmed")
            .addOnSuccessListener {
                Log.d("AdminHomeFragment", "Agendamento confirmado com sucesso!")
                loadDataSafely()
            }
            .addOnFailureListener { e ->
                Log.w("AdminHomeFragment", "Erro ao confirmar o agendamento: ", e)
            }
    }

    private fun cancelBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId).delete()
            .addOnSuccessListener {
                Log.d("CancelBooking", "Agendamento cancelado com sucesso!")
                loadDataSafely()
            }
            .addOnFailureListener { e ->
                Log.w("CancelBooking", "Erro ao cancelar agendamento", e)
            }
    }

    private fun loadBusinessName() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            binding.userNameBusiness.text = "Usuário não autenticado"
            return
        }

        firestore.collection("business")
            .whereEqualTo("ownerId", currentUserUid)
            .get()
            .addOnSuccessListener { documents ->
                val business = documents.firstOrNull()?.toObject(Business::class.java)
                adminViewModel.setBusinessName(business?.name ?: "Nome não disponível")
                loadProfileImage(business?.imageUrl)
            }
            .addOnFailureListener { e ->
                adminViewModel.setBusinessName("Erro ao carregar nome")
                Log.e("AdminHomeFragment", "Erro ao buscar nome do negócio: ", e)
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
