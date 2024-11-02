package com.conect.aplicativoconect.view.ui.client

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R

class HorariosAdapter(
    private val horarios: List<String>,
    private val onHourSelected: (String) -> Unit
) : RecyclerView.Adapter<HorariosAdapter.HorarioViewHolder>() {

    private var selectedHour: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_horario, parent, false)
        return HorarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: HorarioViewHolder, position: Int) {
        val hour = horarios[position]
        holder.bind(hour)

        holder.itemView.setBackgroundColor(
            if (hour == selectedHour) Color.LTGRAY else Color.WHITE
        )

        holder.itemView.setOnClickListener {
            selectedHour = hour
            onHourSelected(hour)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = horarios.size

    inner class HorarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(hour: String) {
            itemView.findViewById<TextView>(R.id.textViewHora).text = hour
        }
    }
}
