package com.conect.aplicativoconect.view.ui.admin

import BookingAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminHomeFragment : Fragment() {

    private lateinit var todayBookingsRecyclerView: RecyclerView
    private lateinit var todayBookingsCountTextView: TextView
    private lateinit var monthBookingsCountTextView: TextView
    private lateinit var todayProfitTextView: TextView
    private lateinit var monthProfitTextView: TextView
    private lateinit var noBookingsMessage: TextView
    private lateinit var noBookingsImage: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var greetingTextView: TextView

    private val bookingRepository = BookingRepository()
    private lateinit var firestore: FirebaseFirestore

    private val adminViewModel: AdminViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialize o Firestore
        firestore = FirebaseFirestore.getInstance()

        // Configuração dos elementos da interface
        todayBookingsRecyclerView = view.findViewById(R.id.todayBookingsRecyclerView)
        todayBookingsCountTextView = view.findViewById(R.id.weekBookingsCount)
        monthBookingsCountTextView = view.findViewById(R.id.monthBookingsCount)
        todayProfitTextView = view.findViewById(R.id.todayProfitTextView)
        monthProfitTextView = view.findViewById(R.id.monthProfitTextView)
        noBookingsMessage = view.findViewById(R.id.noBookingsMessage)
        noBookingsImage = view.findViewById(R.id.noBookingsImage)
        userNameTextView = view.findViewById(R.id.userName_business)
        greetingTextView = view.findViewById(R.id.greetingTextView)

        setupRecyclerView()
        loadData()
        loadBusinessName()

        // Observa mudanças nos ViewModels
        adminViewModel.todayBookingsCount.observe(viewLifecycleOwner) { count ->
            todayBookingsCountTextView.text = count.toString()
        }

        adminViewModel.monthBookingsCount.observe(viewLifecycleOwner) { count ->
            monthBookingsCountTextView.text = count.toString()
        }

        adminViewModel.todayProfit.observe(viewLifecycleOwner) { profit ->
            todayProfitTextView.text = String.format("R$ %.2f", profit)
        }

        adminViewModel.monthProfit.observe(viewLifecycleOwner) { profit ->
            monthProfitTextView.text = String.format("R$ %.2f", profit)
        }

        adminViewModel.businessName.observe(viewLifecycleOwner) { name ->
            userNameTextView.text = name ?: "Nome não disponível"
            setGreeting(name ?: "Usuário")
        }
    }

    private fun setGreeting(name: String) {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when {
            hourOfDay < 6 -> "Boa madrugada!"
            hourOfDay < 12 -> "Bom dia!"
            hourOfDay < 18 -> "Boa tarde!"
            else -> "Boa noite!"
        }

        greetingTextView.text = greeting
    }

    private fun setupRecyclerView() {
        todayBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun loadData() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
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
                    adminViewModel.setTodayBookingsCount(weeklyBookings.size)
                    adminViewModel.setMonthBookingsCount(monthBookings.size)
                    adminViewModel.setTodayProfit(weeklyProfit)
                    adminViewModel.setMonthProfit(monthlyProfit)

                    if (weeklyBookings.isEmpty()) {
                        noBookingsMessage.visibility = View.VISIBLE
                        noBookingsImage.visibility = View.VISIBLE
                        todayBookingsRecyclerView.visibility = View.GONE
                    } else {
                        noBookingsMessage.visibility = View.GONE
                        noBookingsImage.visibility = View.GONE
                        todayBookingsRecyclerView.visibility = View.VISIBLE

                        todayBookingsRecyclerView.adapter = BookingAdapter(
                            weeklyBookings,
                            requireContext(),
                            { bookingId -> confirmBooking(bookingId) },
                            { bookingId -> cancelBooking(bookingId) }
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    noBookingsMessage.text = "Erro ao carregar dados."
                    noBookingsMessage.visibility = View.VISIBLE
                    noBookingsImage.visibility = View.VISIBLE
                    todayBookingsRecyclerView.visibility = View.GONE
                    Log.e("AdminHomeFragment", "Erro ao carregar dados: ", e)
                }
            }
        }
    }

    private fun confirmBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .update("status", "confirmed")
            .addOnSuccessListener {
                Log.d("AdminHomeFragment", "Agendamento confirmado com sucesso!")
                loadData()
            }
            .addOnFailureListener { e ->
                Log.w("AdminHomeFragment", "Erro ao confirmar o agendamento: ", e)
            }
    }

    private fun cancelBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId).delete()
            .addOnSuccessListener {
                Log.d("CancelBooking", "Agendamento cancelado com sucesso!")
                loadData()
            }
            .addOnFailureListener { e ->
                Log.w("CancelBooking", "Erro ao cancelar agendamento", e)
            }
    }

    private fun loadBusinessName() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            userNameTextView.text = "Usuário não autenticado"
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
        val userImageView = view?.findViewById<CircleImageView>(R.id.userImage)
        imageUrl?.let {
            if (userImageView != null) {
                Glide.with(this)
                    .load(it)
                    .placeholder(R.drawable.foto_perfil_generica)
                    .error(R.drawable.foto_perfil_generica)
                    .into(userImageView)
            }
        } ?: run {
            userImageView?.setImageResource(R.drawable.foto_perfil_generica)
        }
    }
}
