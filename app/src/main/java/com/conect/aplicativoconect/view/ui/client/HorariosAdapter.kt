package com.conect.aplicativoconect.view.ui.client

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import android.graphics.Color

class HorariosAdapter(
    private val horarios: List<Int>,
    private val onHourSelected: (Int) -> Unit
) : RecyclerView.Adapter<HorariosAdapter.HorarioViewHolder>() {

    private var selectedHour: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_horario, parent, false)
        return HorarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: HorarioViewHolder, position: Int) {
        val hour = horarios[position]
        holder.bind(hour)

        // Alterar a cor do item selecionado
        holder.itemView.setBackgroundColor(
            if (hour == selectedHour) Color.LTGRAY else Color.WHITE
        )

        holder.itemView.setOnClickListener {
            selectedHour = hour
            onHourSelected(hour)
            notifyDataSetChanged() // Atualizar a visualização
        }
    }

    override fun getItemCount(): Int = horarios.size

    inner class HorarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(hour: Int) {
            itemView.findViewById<TextView>(R.id.textViewHora).text = "$hour:00"
        }
    }
}
