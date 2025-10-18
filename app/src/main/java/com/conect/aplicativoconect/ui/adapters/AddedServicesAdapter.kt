package com.conect.aplicativoconect.ui.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.ServiceType
import com.google.android.material.button.MaterialButton

class AddedServicesAdapter(
    private val addedServices: MutableList<Triple<ServiceType, Double, Int>>,
    private val onRemoveService: (Int) -> Unit // Lambda para remover serviço
) : RecyclerView.Adapter<AddedServicesAdapter.AddedServiceViewHolder>() {

    inner class AddedServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceNameTextView: TextView = itemView.findViewById(R.id.textViewServiceName)
        private val servicePriceEditText: EditText =
            itemView.findViewById(R.id.editTextServicePrice)
        private val serviceDurationEditText: EditText =
            itemView.findViewById(R.id.editTextServiceDuration)
        private val removeServiceButton: MaterialButton =
            itemView.findViewById(R.id.buttonRemoveService)

        fun bind(service: Triple<ServiceType, Double, Int>, position: Int) {
            serviceNameTextView.text = service.first.name
            servicePriceEditText.setText(service.second.toString())
            serviceDurationEditText.setText(service.third.toString())

            // Define um listener para o botão de remoção com confirmação
            removeServiceButton.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Confirmar remoção")
                    .setMessage("Tem certeza que deseja remover o serviço \"${service.first.name}\"?")
                    .setPositiveButton("Remover") { _, _ ->
                        onRemoveService(position)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            // Valida e atualiza o preço quando o campo perde o foco
            servicePriceEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newPrice = servicePriceEditText.text.toString().toDoubleOrNull()
                    if (newPrice != null && newPrice >= 0) {
                        // Atualiza o preço se for válido
                        addedServices[position] =
                            Triple(service.first, newPrice, service.third)
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

            // Valida e atualiza a duração quando o campo perde o foco
            serviceDurationEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newDuration = serviceDurationEditText.text.toString().toIntOrNull()
                    if (newDuration != null && newDuration > 0) {
                        // Atualiza a duração se for válida
                        addedServices[position] =
                            Triple(service.first, service.second, newDuration)
                    } else {
                        // Exibe uma mensagem de erro se a duração for inválida
                        Toast.makeText(
                            itemView.context,
                            "Por favor, insira uma duração válida em minutos.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Restaura o valor anterior no campo de texto
                        serviceDurationEditText.setText(service.third.toString())
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
