package com.conect.aplicativoconect.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Business
import com.conect.aplicativoconect.utils.ImageHelper
import com.conect.aplicativoconect.data.models.OperatingHours

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
        val business = getItem(position)

        // Atualiza o nome e a categoria
        holder.businessName.text = business.name
        holder.businessCategory.text = business.serviceType

        // Formata e atualiza o horário de funcionamento
        holder.operatingHours.text = formatOperatingHours(business.operatingHours)

        // Configura a imagem do negócio com Glide
        loadImage(business.imageUrl, holder.businessImage)

        // Formata e exibe a avaliação média
        holder.ratingTextView.text = formatAverageRating(business.averageRating.toFloat())


        // Configura o botão de agendamento
        holder.bookButton.setOnClickListener { onBusinessClick(business) }
    }

    override fun submitList(list: List<Business>?) {
        // Remove a lógica redundante e permite que o `DiffUtil` cuide das atualizações.
        super.submitList(list?.toList())
    }

    private fun formatOperatingHours(operatingHours: OperatingHours?): String {
        val opening = operatingHours?.opening ?: "N/A"
        val closing = operatingHours?.closing ?: "N/A"
        return "$opening - $closing"
    }

    private fun formatAverageRating(rating: Float?): String {
        return if (rating != null) {
            String.format("%.1f", rating)
        } else {
            "N/A"
        }
    }

    private fun loadImage(imageUrl: String?, imageView: ImageView) {
        ImageHelper.loadBusinessImage(context, imageUrl, imageView, 300, 300) // ✅ OTIMIZADO: Helper centralizado
    }
}

// BusinessDiffCallback movido para arquivo separado - BusinessDiffCallback.kt
