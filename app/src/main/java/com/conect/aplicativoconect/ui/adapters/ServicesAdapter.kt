package com.conect.aplicativoconect.ui.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Service
import com.conect.aplicativoconect.ui.activities.SelecionarHorarioActivity
import java.io.Serializable

class ServicesAdapter(
    private val context: Context,
    private val services: List<Service>,
    private val companyId: String // Inclui o ID da empresa para enviar na intent
) : RecyclerView.Adapter<ServicesAdapter.ServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_about_company, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        holder.bind(service)
    }

    override fun getItemCount(): Int = services.size

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceName: TextView = itemView.findViewById(R.id.serviceName)
        private val servicePrice: TextView = itemView.findViewById(R.id.servicePrice)
        private val bookButton: Button = itemView.findViewById(R.id.bookButton)

        fun bind(service: Service) {
            serviceName.text = service.name
            servicePrice.text = "R$ ${service.price}"

            // Configura o clique do botão Agendar
            bookButton.setOnClickListener {
                val intent = Intent(context, SelecionarHorarioActivity::class.java).apply {
                    putExtra(
                        "selectedService",
                        service as Serializable
                    ) // Passa o serviço selecionado
                    putExtra("companyId", companyId) // Passa o ID da empresa
                }
                context.startActivity(intent)
            }
        }
    }
}
