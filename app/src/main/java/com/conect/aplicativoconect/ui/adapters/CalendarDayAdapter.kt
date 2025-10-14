package com.conect.aplicativoconect.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import java.text.SimpleDateFormat
import java.util.*

class CalendarDayAdapter(
    private val days: List<Date>,
    private val selectedDate: Date?,
    private val onDayClick: (Date?) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val date = days[position]
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        val weekFormat = SimpleDateFormat("EEE", Locale("pt", "BR"))
        holder.tvDayNumber.text = dayFormat.format(date)
        holder.tvDayWeek.text = weekFormat.format(date).capitalize(Locale.getDefault())

        holder.itemView.isSelected = selectedPosition == position
        holder.itemView.setOnClickListener {
            if (selectedPosition == position) {
                // Deseleciona se clicar novamente
                selectedPosition = -1
                notifyDataSetChanged()
                onDayClick(null)
            } else {
                selectedPosition = position
                notifyDataSetChanged()
                onDayClick(date)
            }
        }
    }

    override fun getItemCount(): Int = days.size

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        val tvDayWeek: TextView = itemView.findViewById(R.id.tvDayWeek)
    }
}
