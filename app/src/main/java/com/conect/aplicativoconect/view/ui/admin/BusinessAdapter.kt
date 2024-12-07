package com.conect.aplicativoconect.view.ui.admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.data.model.OperatingHours

class BusinessAdapter(
    private val context: Context,
    private val onBusinessClick: (Business) -> Unit
) : ListAdapter<Business, BusinessAdapter.BusinessViewHolder>(BusinessDiffCallback()) {

    inner class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val businessName: TextView = itemView.findViewById(R.id.businessName)
        val businessCategory: TextView = itemView.findViewById(R.id.businessAddress)
        val operatingHours: TextView = itemView.findViewById(R.id.businessOperatingHours)
        val businessImage: ImageView = itemView.findViewById(R.id.businessImage)
        val ratingTextView: TextView = itemView.findViewById(R.id.businessAverageRating)
        val bookButton: Button = itemView.findViewById(R.id.bookButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_business, parent, false)
        return BusinessViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val business = getItem(position) // Usamos getItem() em vez de acessar diretamente a lista
        holder.businessName.text = business.name
        holder.businessCategory.text = business.serviceType
        holder.operatingHours.text = formatOperatingHours(business.operatingHours)

        Glide.with(context)
            .load(business.imageUrl)
            .placeholder(R.drawable.foto_perfil_generica)
            .into(holder.businessImage)

        holder.ratingTextView.text =
            business.averageRating.let { String.format("%.1f", it) }

        holder.bookButton.setOnClickListener {
            onBusinessClick(business)
        }
    }

    private fun formatOperatingHours(operatingHours: OperatingHours?): String {
        val opening = operatingHours?.opening ?: "N/A"
        val closing = operatingHours?.closing ?: "N/A"
        return "$opening - $closing"
    }
}

// Criação do DiffUtil.ItemCallback para comparar os itens
class BusinessDiffCallback : DiffUtil.ItemCallback<Business>() {
    override fun areItemsTheSame(oldItem: Business, newItem: Business): Boolean {
        // Comparação de ID (ou outro campo único) para garantir que estamos lidando com o mesmo item
        return oldItem.cpf == newItem.cpf  // Usando CPF como identificador único
    }

    override fun areContentsTheSame(oldItem: Business, newItem: Business): Boolean {
        // Verificação de igualdade dos dados do item
        return oldItem == newItem
    }
}
