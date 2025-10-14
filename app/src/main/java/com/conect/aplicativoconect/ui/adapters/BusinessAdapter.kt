package com.conect.aplicativoconect.ui.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Business
import com.conect.aplicativoconect.utils.ImageHelper
import com.conect.aplicativoconect.data.models.OperatingHours
import java.util.Calendar
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
    private val onBusinessClick: (Business) -> Unit,
    private val onFavoriteClick: ((Business, Boolean) -> Unit)? = null
) : ListAdapter<BusinessWithDistance, BusinessAdapter.BusinessViewHolder>(BusinessWithDistanceDiffCallback()) {

    // 🎯 Variáveis para localização do usuário (para cálculo de distância)
    private var userLat: Double? = null
    private var userLon: Double? = null
    
    // ❤️ Lista de estabelecimentos favoritos
    private val favoriteBusinessIds = mutableSetOf<String>()

    inner class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val businessName: TextView = itemView.findViewById(R.id.businessName)
        val businessCategory: TextView = itemView.findViewById(R.id.businessAddress)
        val operatingHours: TextView = itemView.findViewById(R.id.businessOperatingHours)
        val businessImage: ImageView = itemView.findViewById(R.id.businessImage)
        val ratingTextView: TextView = itemView.findViewById(R.id.businessAverageRating)
    val bookButton: TextView = itemView.findViewById(R.id.bookButton)
        
        // 🆕 NOVOS ELEMENTOS DO DESIGN MODERNO
        val distanceTextView: TextView? = itemView.findViewById(R.id.businessDistance)
        val statusBadge: TextView? = itemView.findViewById(R.id.businessStatus)
        val favoriteIcon: ImageView? = itemView.findViewById(R.id.favoriteIcon)
    }
    
    // 🚀 NOVA FUNÇÃO: Definir localização do usuário para cálculos de distância
    fun setUserLocation(lat: Double?, lon: Double?) {
        userLat = lat
        userLon = lon
    }
    
    // ❤️ FUNÇÕES DE FAVORITOS
    fun setFavoriteBusinesses(favoriteIds: Set<String>) {
        favoriteBusinessIds.clear()
        favoriteBusinessIds.addAll(favoriteIds)
        notifyDataSetChanged()
    }
    
    private fun updateFavoriteIcon(favoriteIcon: ImageView, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteIcon.setImageResource(android.R.drawable.btn_star_big_on)
            favoriteIcon.setColorFilter(ContextCompat.getColor(context, R.color.warning))
        } else {
            favoriteIcon.setImageResource(android.R.drawable.btn_star_big_off)
            favoriteIcon.setColorFilter(ContextCompat.getColor(context, R.color.cinza))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_business, parent, false)
        return BusinessViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val businessWithDistance = getItem(position)
        val business = businessWithDistance.business

        // 🏢 Nome e categoria
        holder.businessName.text = business.name
        holder.businessCategory.text = business.serviceType

        // 🕒 Horário de funcionamento
        holder.operatingHours.text = formatOperatingHours(business.operatingHours)

        // 📸 Imagem do estabelecimento
        loadImage(business.imageUrl, holder.businessImage)

        // ⭐ Avaliação
        holder.ratingTextView.text = formatAverageRating(business.averageRating.toFloat())

        // 📍 Distância (se disponível)
        holder.distanceTextView?.let { distanceTV ->
            businessWithDistance.distanceText?.let { distance ->
                distanceTV.text = distance
                distanceTV.visibility = View.VISIBLE
                distanceTV.parent?.let { parent ->
                    (parent as? View)?.visibility = View.VISIBLE
                }
            } ?: run {
                distanceTV.visibility = View.GONE
                distanceTV.parent?.let { parent ->
                    (parent as? View)?.visibility = View.GONE
                }
            }
        }

        // 🏷️ Status Badge (Aberto/Fechado)
        holder.statusBadge?.let { statusBadge ->
            val isOpen = isBusinessOpen(business.operatingHours)
            statusBadge.text = if (isOpen) "ABERTO" else "FECHADO"
            statusBadge.setBackgroundColor(
                ContextCompat.getColor(
                    context, 
                    if (isOpen) R.color.success else R.color.error
                )
            )
        }

        // ❤️ Favoritos
        holder.favoriteIcon?.let { favoriteIcon ->
            val isFavorite = favoriteBusinessIds.contains(business.ownerId)
            updateFavoriteIcon(favoriteIcon, isFavorite)
            
            favoriteIcon.setOnClickListener {
                val newFavoriteState = !isFavorite
                if (newFavoriteState) {
                    favoriteBusinessIds.add(business.ownerId)
                } else {
                    favoriteBusinessIds.remove(business.ownerId)
                }
                updateFavoriteIcon(favoriteIcon, newFavoriteState)
                onFavoriteClick?.invoke(business, newFavoriteState)
            }
        }
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

    // 🕒 FUNÇÃO: Verificar se o estabelecimento está aberto
    private fun isBusinessOpen(operatingHours: OperatingHours?): Boolean {
        if (operatingHours?.opening == null || operatingHours.closing == null) {
            return false // Se não tem horário definido, considera fechado
        }

        try {
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute

            // Parse do horário de abertura
            val openingParts = operatingHours.opening.split(":")
            val openingTimeInMinutes = openingParts[0].toInt() * 60 + openingParts[1].toInt()

            // Parse do horário de fechamento
            val closingParts = operatingHours.closing.split(":")
            val closingTimeInMinutes = closingParts[0].toInt() * 60 + closingParts[1].toInt()

            return currentTimeInMinutes in openingTimeInMinutes..closingTimeInMinutes
        } catch (e: Exception) {
            return false // Em caso de erro no parse, considera fechado
        }
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
