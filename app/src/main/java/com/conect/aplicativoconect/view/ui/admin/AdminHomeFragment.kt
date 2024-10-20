package com.conect.aplicativoconect.view.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.repository.BookingRepository
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

    private val bookingRepository = BookingRepository()

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

        setupRecyclerView()
        loadData()
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
                    // Update UI elements
                    todayBookingsCountTextView.text = todayBookings.size.toString()
                    monthBookingsCountTextView.text = monthBookings.size.toString()
                    todayProfitTextView.text = String.format("R$ %.2f", todayProfit)
                    monthProfitTextView.text = String.format("R$ %.2f", monthProfit)

                    // Show bookings in RecyclerView or a message if no bookings exist
                    if (todayBookings.isEmpty()) {
                        noBookingsMessage.visibility = View.VISIBLE
                    } else {
                        noBookingsMessage.visibility = View.GONE
                        todayBookingsRecyclerView.adapter = BookingAdapter(todayBookings)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Handle errors
                    noBookingsMessage.text = "Erro ao carregar dados."
                    noBookingsMessage.visibility = View.VISIBLE
                }
            }
        }
    }
}
