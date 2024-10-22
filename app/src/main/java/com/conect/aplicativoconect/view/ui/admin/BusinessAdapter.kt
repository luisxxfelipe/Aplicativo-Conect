package com.conect.aplicativoconect.view.ui.admin

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.ui.client.EmpresaDetalhesActivity
import com.google.firebase.firestore.FirebaseFirestore

class BusinessAdapter(
    private val context: Context,
    private val businessList: List<Business>,
    private val onBusinessClick: (Business) -> Unit // Função de clique
) : RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {

    class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val businessName: TextView = itemView.findViewById(R.id.businessName)
        val businessAddress: TextView = itemView.findViewById(R.id.businessAddress)
        val businessPhone: TextView = itemView.findViewById(R.id.businessPhone)
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

        // Definindo a ação de clique no botão
        holder.bookButton.setOnClickListener {
            fetchBusinessIdAndOpenDetails(business.name) // Passa o nome da empresa
        }
    }

    private fun fetchBusinessIdAndOpenDetails(businessName: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("business")
            .whereEqualTo("name", businessName)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        // Aqui você obteve o ID da empresa
                        val businessId = document.id

                        // Agora inicia a EmpresaDetalhesActivity passando o ID
                        val intent = Intent(context, EmpresaDetalhesActivity::class.java).apply {
                            putExtra("companyId", businessId) // Passa o ID da empresa
                        }
                        context.startActivity(intent)
                        break // Sai do loop após encontrar o primeiro ID
                    }
                } else {
                    Log.d("BusinessAdapter", "No matching business found")
                }
            }
            .addOnFailureListener { e ->
                Log.w("BusinessAdapter", "Error getting documents: ", e)
            }
    }

    override fun getItemCount(): Int {
        return businessList.size
    }
}
