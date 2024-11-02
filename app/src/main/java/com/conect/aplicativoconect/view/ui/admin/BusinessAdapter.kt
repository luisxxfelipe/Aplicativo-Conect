package com.conect.aplicativoconect.view.ui.admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.data.model.OperatingHours

class BusinessAdapter(
    private val context: Context,
    private val businessList: List<Business>,
    private val onBusinessClick: (Business) -> Unit
) : RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {

    inner class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val businessName: TextView = itemView.findViewById(R.id.businessName)
        val businessCategory: TextView =
            itemView.findViewById(R.id.businessAddress) // Reaproveitando o campo
        val operatingHours: TextView = itemView.findViewById(R.id.businessOperatingHours)
        val businessImage: ImageView = itemView.findViewById(R.id.businessImage)
        val bookButton: Button = itemView.findViewById(R.id.bookButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_business, parent, false) // Use o layout correto aqui
        return BusinessViewHolder(view)
    }


    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val business = businessList[position]

        holder.businessName.text = business.name
        holder.businessCategory.text = business.serviceType
        holder.operatingHours.text = formatOperatingHours(business.operatingHours)

        Glide.with(context)
            .load(business.imageUrl)
            .placeholder(R.drawable.foto_perfil_generica)
            .into(holder.businessImage)

        holder.bookButton.setOnClickListener {
            onBusinessClick(business)
        }


    }

    // Função ajustada para trabalhar com a data class OperatingHours
    private fun formatOperatingHours(operatingHours: OperatingHours?): String {
        val opening = operatingHours?.opening ?: "N/A"
        val closing = operatingHours?.closing ?: "N/A"
        return "$opening - $closing"
    }

    override fun getItemCount(): Int = businessList.size
}
