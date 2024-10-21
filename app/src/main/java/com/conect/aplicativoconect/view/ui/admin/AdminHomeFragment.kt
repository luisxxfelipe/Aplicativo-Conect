package com.conect.aplicativoconect.view.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.data.repository.BookingRepository
import com.conect.aplicativoconect.view.viewmodel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminHomeFragment : Fragment() {

    private lateinit var todayBookingsRecyclerView: RecyclerView
    private lateinit var todayBookingsCountTextView: TextView
    private lateinit var monthBookingsCountTextView: TextView
    private lateinit var todayProfitTextView: TextView
    private lateinit var monthProfitTextView: TextView
    private lateinit var noBookingsMessage: TextView
    private lateinit var userNameTextView: TextView

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

        todayBookingsRecyclerView = view.findViewById(R.id.todayBookingsRecyclerView)
        todayBookingsCountTextView = view.findViewById(R.id.todayBookingsCount)
        monthBookingsCountTextView = view.findViewById(R.id.monthBookingsCount)
        todayProfitTextView = view.findViewById(R.id.todayProfit)
        monthProfitTextView = view.findViewById(R.id.monthProfit)
        noBookingsMessage = view.findViewById(R.id.noBookingsMessage)
        userNameTextView = view.findViewById(R.id.userName_business)

        setupRecyclerView()
        loadData()
        loadBusinessName()

        // Observa os dados do ViewModel
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
        }
    }

    private fun setupRecyclerView() {
        todayBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch data from the repository
                val todayBookings = bookingRepository.getTodayBookings()
                val monthBookings = bookingRepository.getMonthBookings()
                val todayProfit = bookingRepository.getTodayProfit()
                val monthProfit = bookingRepository.getMonthProfit()

                withContext(Dispatchers.Main) {
                    // Atualiza dados no ViewModel
                    adminViewModel.setTodayBookingsCount(todayBookings.size)
                    adminViewModel.setMonthBookingsCount(monthBookings.size)
                    adminViewModel.setTodayProfit(todayProfit)
                    adminViewModel.setMonthProfit(monthProfit)

                    // Mostrar agendamentos no RecyclerView ou mensagem se não houver
                    if (todayBookings.isEmpty()) {
                        noBookingsMessage.visibility = View.VISIBLE
                    } else {
                        noBookingsMessage.visibility = View.GONE
                        todayBookingsRecyclerView.adapter = BookingAdapter(todayBookings)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Tratar erros
                    noBookingsMessage.text = "Erro ao carregar dados."
                    noBookingsMessage.visibility = View.VISIBLE
                    Log.e("AdminHomeFragment", "Erro ao carregar dados: ", e)
                }
            }
        }
    }

    private fun loadBusinessName() {
        firestore = FirebaseFirestore.getInstance()
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            userNameTextView.text = "Usuário não autenticado"
            return
        }

        firestore.collection("business").document(currentUserUid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val business = document.toObject(Business::class.java)
                    adminViewModel.setBusinessName(business?.name ?: "Nome não disponível")
                } else {
                    adminViewModel.setBusinessName("Nome não disponível")
                }
            }
            .addOnFailureListener { e ->
                adminViewModel.setBusinessName("Erro ao carregar o nome")
                Log.e("AdminHomeFragment", "Erro ao buscar o nome do negócio: ", e)
            }
    }
}
