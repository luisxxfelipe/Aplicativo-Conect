package com.conect.aplicativoconect.view.ui.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Service

class ServicesAdapter(
    private val servicesList: List<Service>,
    private val onServiceClick: (Service) -> Unit,
    private val onBookClick: (Service) -> Unit
) : RecyclerView.Adapter<ServicesAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
        val servicePriceTextView: TextView = itemView.findViewById(R.id.servicePriceTextView)
        val bookButton: Button = itemView.findViewById(R.id.bookButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_about_company, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = servicesList[position]

        // Define o nome e preço do serviço nos TextViews
        holder.serviceTextView.text = service.name
        holder.servicePriceTextView.text = "R$ ${service.price}"

        // Ação ao clicar no item do serviço
        holder.itemView.setOnClickListener {
            onServiceClick(service)
        }

        // Ação ao clicar no botão "Agendar"
        holder.bookButton.setOnClickListener {
            onBookClick(service)
        }
    }

    override fun getItemCount(): Int {
        return servicesList.size
    }
}
