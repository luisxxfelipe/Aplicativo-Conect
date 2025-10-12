package com.conect.aplicativoconect.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.Coupon
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🎯 ADAPTER: Lista de Cupons
 * Exibe cupons disponíveis do usuário
 */
class CouponAdapter(
    private val onCouponClick: (Coupon) -> Unit
) : RecyclerView.Adapter<CouponAdapter.CouponViewHolder>() {

    private var coupons = listOf<Coupon>()

    fun updateCoupons(newCoupons: List<Coupon>) {
        coupons = newCoupons
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coupon, parent, false)
        return CouponViewHolder(view)
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        holder.bind(coupons[position])
    }

    override fun getItemCount() = coupons.size

    inner class CouponViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val couponCode: TextView = itemView.findViewById(R.id.couponCode)
        private val couponDiscount: TextView = itemView.findViewById(R.id.couponDiscount)
        private val couponExpiry: TextView = itemView.findViewById(R.id.couponExpiry)
        private val couponStatus: TextView = itemView.findViewById(R.id.couponStatus)

        fun bind(coupon: Coupon) {
            couponCode.text = coupon.code
            couponDiscount.text = "${coupon.discountPercent}% OFF"
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            couponExpiry.text = "Válido até: ${dateFormat.format(Date(coupon.expiresAt))}"
            
            couponStatus.text = if (coupon.isUsed) {
                couponStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                "USADO"
            } else {
                couponStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                "DISPONÍVEL"
            }

            itemView.setOnClickListener {
                if (!coupon.isUsed) {
                    onCouponClick(coupon)
                }
            }

            // Visual feedback para cupons usados
            itemView.alpha = if (coupon.isUsed) 0.6f else 1.0f
        }
    }
}