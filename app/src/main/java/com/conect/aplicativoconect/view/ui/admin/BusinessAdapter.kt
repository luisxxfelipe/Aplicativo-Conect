package com.conect.aplicativoconect.view.ui.admin

import android.content.Context
import android.content.Intent
import android.util.Log
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
import com.conect.aplicativoconect.view.ui.client.EmpresaDetalhesActivity
import com.google.firebase.firestore.FirebaseFirestore

class BusinessAdapter(
    private val context: Context,
    private val businessList: List<Business>,
    private val onBusinessClick: (Business) -> Unit // Adicionado
) : RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {

    class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val businessName: TextView = itemView.findViewById(R.id.businessName)
        val businessAddress: TextView = itemView.findViewById(R.id.businessAddress)
        val businessPhone: TextView = itemView.findViewById(R.id.businessPhone)
        val businessImage: ImageView = itemView.findViewById(R.id.businessImage) // Adicionado
        val bookButton: Button = itemView.findViewById(R.id.bookButton) // Referência ao botão
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_business, parent, false)
        return BusinessViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val business = businessList[position]
        holder.businessName.text = business.name
        holder.businessAddress.text = business.address
        holder.businessPhone.text = business.phone

        // Carregando a imagem da empresa usando Glide
        Glide.with(context)
            .load(business.imageUrl) // A URL da imagem
            .placeholder(R.drawable.imagem_negocios) // Placeholder enquanto carrega
            .error(R.drawable.imagem_negocios) // Imagem de erro
            .into(holder.businessImage)

        // Definindo a ação de clique no botão
        holder.bookButton.setOnClickListener {
            onBusinessClick(business) // Chama a função de callback
        }

    }

    override fun getItemCount(): Int {
        return businessList.size
    }
}
