package com.conect.aplicativoconect.view.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Business

class BusinessAdapter(private val businessList: List<Business>) :
    RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {

    class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val businessName: TextView = itemView.findViewById(R.id.businessName)
        val businessAddress: TextView = itemView.findViewById(R.id.businessAddress) // Adicionando para mostrar o endereço
        val businessPhone: TextView = itemView.findViewById(R.id.businessPhone) // Adicionando para mostrar o telefone
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_business, parent, false)
        return BusinessViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val business = businessList[position]
        holder.businessName.text = business.name
        holder.businessAddress.text = business.address // Mostrando o endereço
        holder.businessPhone.text = business.phone // Mostrando o telefone
    }

    override fun getItemCount(): Int {
        return businessList.size
    }
}
