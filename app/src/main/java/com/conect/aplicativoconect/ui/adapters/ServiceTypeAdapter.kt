package com.conect.aplicativoconect.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.ServiceType

class ServiceTypeAdapter(
    val serviceTypes: List<ServiceType>,
    private val onServiceSelected: (ServiceType) -> Unit // Lambda para tratar a seleção
) : RecyclerView.Adapter<ServiceTypeAdapter.ServiceTypeViewHolder>() {

    private var selectedService: ServiceType? = null // Para armazenar o serviço selecionado

    inner class ServiceTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceNameTextView: TextView = itemView.findViewById(R.id.textViewServiceName)
        private val serviceCheckBox: CheckBox = itemView.findViewById(R.id.checkBoxService)

        fun bind(serviceType: ServiceType) {
            serviceNameTextView.text = serviceType.name
            serviceCheckBox.isChecked = selectedService == serviceType

            // Define a lógica para o CheckBox
            serviceCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (selectedService != null) {
                        // Desmarcar o serviço anteriormente selecionado
                        notifyItemChanged(serviceTypes.indexOf(selectedService))
                    }
                    selectedService = serviceType // Atualiza o serviço selecionado
                    onServiceSelected(serviceType) // Notifica a seleção
                } else if (selectedService == serviceType) {
                    // Se o serviço selecionado for desmarcado
                    selectedService = null // Reseta o serviço selecionado
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceTypeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_type, parent, false)
        return ServiceTypeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceTypeViewHolder, position: Int) {
        holder.bind(serviceTypes[position])
    }

    override fun getItemCount(): Int = serviceTypes.size

    // Método para obter o serviço selecionado
    fun getSelectedService(): ServiceType? {
        return selectedService // Retorna o serviço selecionado
    }

    fun clearSelection() {
        selectedService = null
        notifyDataSetChanged()
    }

}
