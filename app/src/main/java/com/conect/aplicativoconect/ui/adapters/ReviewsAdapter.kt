package com.conect.aplicativoconect.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.ui.viewmodels.CompanyViewModel.ReviewItem

class ReviewsAdapter : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    private val reviewsList = mutableListOf<ReviewItem>()

    // Atualiza a lista de avaliações e notifica a RecyclerView para exibir os novos dados
    fun submitList(reviews: List<ReviewItem>) {
        reviewsList.clear()
        reviewsList.addAll(reviews)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review_card, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviewsList[position])
    }

    override fun getItemCount(): Int = reviewsList.size

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        private val ratingQuality: RatingBar = itemView.findViewById(R.id.ratingQuality)
        private val ratingPunctuality: RatingBar = itemView.findViewById(R.id.ratingPunctuality)
        private val ratingService: RatingBar = itemView.findViewById(R.id.ratingService)
        private val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)

        fun bind(review: ReviewItem) {
            userNameTextView.text = review.name
            ratingQuality.rating = review.quality.toFloat()
            ratingPunctuality.rating = review.punctuality.toFloat()
            ratingService.rating = review.service.toFloat()
            commentTextView.text = review.comment
        }
    }
}