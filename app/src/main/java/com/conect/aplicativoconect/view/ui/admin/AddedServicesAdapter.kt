package com.conect.aplicativoconect.view.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.ServiceType

class AddedServicesAdapter(
    private val services: MutableList<Pair<ServiceType, Double>>, // Tornado mutável para permitir edição
    private val onPriceChange: (ServiceType, Double) -> Unit
) : RecyclerView.Adapter<AddedServicesAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceNameTextView: TextView = itemView.findViewById(R.id.textViewServiceName)
        private val priceEditText: EditText = itemView.findViewById(R.id.editTextServicePrice)

        fun bind(service: Pair<ServiceType, Double>) {
            serviceNameTextView.text = service.first.name
            priceEditText.setText(service.second.toString())

            priceEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newPrice = priceEditText.text.toString().toDoubleOrNull()
                    if (newPrice != null) {
                        onPriceChange(service.first, newPrice)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_added_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(services[position])
    }

    override fun getItemCount(): Int = services.size
}
