package com.conect.aplicativoconect.view.ui.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R

class ServicesAdapter(
    private val servicesList: List<String>,
    private val onServiceClick: (String) -> Unit,
    private val onBookClick: (String) -> Unit
) : RecyclerView.Adapter<ServicesAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
        val bookButton: Button = itemView.findViewById(R.id.bookButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = servicesList[position]
        holder.serviceTextView.text = service

        holder.itemView.setOnClickListener {
            onServiceClick(service)
        }

        holder.bookButton.setOnClickListener {
            onBookClick(service)
        }
    }

    override fun getItemCount(): Int {
        return servicesList.size
    }
}
