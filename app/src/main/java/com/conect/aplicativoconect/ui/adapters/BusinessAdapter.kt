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
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2
import kotlin.math.sqrt

// 🎯 Extensão para Business com informações de distância
data class BusinessWithDistance(
    val business: Business,
    val distanceKm: Double? = null,
    val distanceText: String? = null
)

class BusinessAdapter(
    private val context: Context,
    private val onBusinessClick: (Business) -> Unit
) : ListAdapter<BusinessWithDistance, BusinessAdapter.BusinessViewHolder>(BusinessWithDistanceDiffCallback()) {

    // 🎯 Variáveis para localização do usuário (para cálculo de distância)
    private var userLat: Double? = null
    private var userLon: Double? = null

    inner class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val businessName: TextView = itemView.findViewById(R.id.businessName)
        val businessCategory: TextView = itemView.findViewById(R.id.businessAddress)
        val operatingHours: TextView = itemView.findViewById(R.id.businessOperatingHours)
        val businessImage: ImageView = itemView.findViewById(R.id.businessImage)
        val ratingTextView: TextView = itemView.findViewById(R.id.businessAverageRating)
        val bookButton: Button = itemView.findViewById(R.id.bookButton)
        // 🆕 NOVO: TextView para mostrar distância
        val distanceTextView: TextView? = itemView.findViewById(R.id.businessDistance)
    }
    
    // 🚀 NOVA FUNÇÃO: Definir localização do usuário para cálculos de distância
    fun setUserLocation(lat: Double?, lon: Double?) {
        userLat = lat
        userLon = lon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_business, parent, false)
        return BusinessViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val businessWithDistance = getItem(position)
        val business = businessWithDistance.business

        // Atualiza o nome e a categoria
        holder.businessName.text = business.name
        holder.businessCategory.text = business.serviceType

        // Formata e atualiza o horário de funcionamento
        holder.operatingHours.text = formatOperatingHours(business.operatingHours)

        // Configura a imagem do negócio com Glide
        loadImage(business.imageUrl, holder.businessImage)

        // Formata e exibe a avaliação média
        holder.ratingTextView.text = formatAverageRating(business.averageRating.toFloat())

        // 🆕 NOVO: Mostrar distância se disponível
        holder.distanceTextView?.let { distanceTV ->
            businessWithDistance.distanceText?.let { distance ->
                distanceTV.text = distance
                distanceTV.visibility = View.VISIBLE
                // Mostrar o container pai também
                distanceTV.parent?.let { parent ->
                    (parent as? View)?.visibility = View.VISIBLE
                }
            } ?: run {
                distanceTV.visibility = View.GONE
                // Ocultar o container pai também
                distanceTV.parent?.let { parent ->
                    (parent as? View)?.visibility = View.GONE
                }
            }
        }

        // Configura o botão de agendamento
        holder.bookButton.setOnClickListener { onBusinessClick(business) }
    }

    override fun submitList(list: List<BusinessWithDistance>?) {
        // Remove a lógica redundante e permite que o `DiffUtil` cuide das atualizações.
        super.submitList(list?.toList())
    }
    
    // 🎯 FUNÇÃO para converter List<Business> para List<BusinessWithDistance>
    fun submitBusinessList(businesses: List<Business>) {
        val businessesWithDistance = businesses.map { business ->
            val distanceText = if (userLat != null && userLon != null && 
                                   business.latitude != 0.0 && business.longitude != 0.0) {
                val distance = calculateDistance(userLat!!, userLon!!, business.latitude, business.longitude)
                when {
                    distance < 1.0 -> "${(distance * 1000).toInt()}m"
                    distance < 10.0 -> String.format("%.1fkm", distance)
                    else -> "${distance.toInt()}km"
                }
            } else null
            
            BusinessWithDistance(
                business = business,
                distanceKm = if (userLat != null && userLon != null && 
                                 business.latitude != 0.0 && business.longitude != 0.0) {
                    calculateDistance(userLat!!, userLon!!, business.latitude, business.longitude)
                } else null,
                distanceText = distanceText
            )
        }
        
        submitList(businessesWithDistance)
    }
    
    // 🎯 Função de cálculo de distância (mesmo algoritmo do Fragment)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Raio da Terra em km
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * 
                cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
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

// 🎯 NOVO DiffCallback para BusinessWithDistance
class BusinessWithDistanceDiffCallback : DiffUtil.ItemCallback<BusinessWithDistance>() {
    override fun areItemsTheSame(oldItem: BusinessWithDistance, newItem: BusinessWithDistance): Boolean {
        return oldItem.business.ownerId == newItem.business.ownerId
    }

    override fun areContentsTheSame(oldItem: BusinessWithDistance, newItem: BusinessWithDistance): Boolean {
        return oldItem.business == newItem.business && 
               oldItem.distanceKm == newItem.distanceKm &&
               oldItem.distanceText == newItem.distanceText
    }
}
