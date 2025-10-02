package com.conect.aplicativoconect.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import com.conect.aplicativoconect.data.models.Business

class BusinessDiffCallback : DiffUtil.ItemCallback<Business>() {
    override fun areItemsTheSame(oldItem: Business, newItem: Business): Boolean {
        // Comparação de ID para garantir que estamos lidando com o mesmo item
        return oldItem.cpf == newItem.cpf
    }

    override fun areContentsTheSame(oldItem: Business, newItem: Business): Boolean {
        // Verificação de igualdade dos dados do item
        return oldItem == newItem
    }
}