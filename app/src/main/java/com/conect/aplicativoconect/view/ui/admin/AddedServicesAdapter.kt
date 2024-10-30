package com.conect.aplicativoconect.view.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Service

class AddedServicesAdapter(
    private val addedServices: MutableList<Service>, // Usando a classe Service
    private val onRemoveService: (Int) -> Unit // Lambda para remover serviço
) : RecyclerView.Adapter<AddedServicesAdapter.AddedServiceViewHolder>() {

    inner class AddedServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceNameTextView: TextView = itemView.findViewById(R.id.textViewServiceName)
        private val servicePriceEditText: EditText = itemView.findViewById(R.id.editTextServicePrice)
        private val removeServiceButton: ImageButton = itemView.findViewById(R.id.buttonRemoveService)

        fun bind(service: Service, position: Int) {
            serviceNameTextView.text = service.name
            servicePriceEditText.setText(service.price.toString())

            removeServiceButton.setOnClickListener {
                onRemoveService(position) // Notifica a remoção
            }

            // Atualizar o preço ao sair do campo de texto
            servicePriceEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newPrice = servicePriceEditText.text.toString().toDoubleOrNull()
                    if (newPrice != null) {
                        addedServices[position] = service.copy(price = newPrice) // Atualiza o preço
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddedServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_added_service, parent, false)
        return AddedServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddedServiceViewHolder, position: Int) {
        holder.bind(addedServices[position], position)
    }

    override fun getItemCount(): Int = addedServices.size
}
