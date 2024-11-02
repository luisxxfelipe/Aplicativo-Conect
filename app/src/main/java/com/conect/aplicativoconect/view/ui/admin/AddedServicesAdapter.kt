package com.conect.aplicativoconect.view.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.ServiceType

class AddedServicesAdapter(
    private val addedServices: MutableList<Pair<ServiceType, Double>>,
    private val onRemoveService: (Int) -> Unit // Lambda para remover serviço
) : RecyclerView.Adapter<AddedServicesAdapter.AddedServiceViewHolder>() {

    inner class AddedServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceNameTextView: TextView = itemView.findViewById(R.id.textViewServiceName)
        private val servicePriceEditText: EditText =
            itemView.findViewById(R.id.editTextServicePrice)
        private val removeServiceButton: ImageButton =
            itemView.findViewById(R.id.buttonRemoveService)

        fun bind(service: Pair<ServiceType, Double>, position: Int) {
            serviceNameTextView.text = service.first.name
            servicePriceEditText.setText(service.second.toString())

            // Define um listener para o botão de remoção
            removeServiceButton.setOnClickListener {
                onRemoveService(position) // Notifica a remoção
            }

            // Valida e atualiza o preço quando o campo perde o foco
            servicePriceEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newPrice = servicePriceEditText.text.toString().toDoubleOrNull()
                    if (newPrice != null && newPrice >= 0) {
                        // Atualiza o preço se for válido
                        addedServices[position] = Pair(service.first, newPrice)
                    } else {
                        // Exibe uma mensagem de erro se o preço for inválido
                        Toast.makeText(
                            itemView.context,
                            "Por favor, insira um preço válido.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Restaura o valor anterior no campo de texto
                        servicePriceEditText.setText(service.second.toString())
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
