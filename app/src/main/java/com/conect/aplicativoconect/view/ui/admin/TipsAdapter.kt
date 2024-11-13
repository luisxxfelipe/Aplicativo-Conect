package com.conect.aplicativoconect.view.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R

class TipsAdapter(private val tips: List<String>) : RecyclerView.Adapter<TipsAdapter.TipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tip, parent, false)
        return TipViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        holder.bind(tips[position])
    }

    override fun getItemCount(): Int = tips.size

    class TipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tipTextView: TextView = itemView.findViewById(R.id.tipTextView)

        fun bind(tip: String) {
            tipTextView.text = tip
        }
    }
}