package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.google.firebase.firestore.FirebaseFirestore

class ReviewsFragment : Fragment() {

    private lateinit var companyId: String
    private lateinit var firestore: FirebaseFirestore
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var reviewsAdapter: ReviewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            companyId = it.getString("companyId") ?: ""
            Log.d(
                "ReviewsFragment",
                "companyId recebido: $companyId"
            )  // Log para verificar o companyId
        }
        firestore = FirebaseFirestore.getInstance()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reviews, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reviewsRecyclerView = view.findViewById(R.id.reviewsRecyclerView)
        reviewsRecyclerView.layoutManager = LinearLayoutManager(context)
        reviewsAdapter = ReviewsAdapter()
        reviewsRecyclerView.adapter = reviewsAdapter

        fetchReviews()
    }

    private fun fetchReviews() {
        firestore.collection("bookings")
            .whereEqualTo("companyId", companyId)
            .whereNotEqualTo("rating", null)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d("fetchReviews", "Nenhuma avaliação encontrada para companyId: $companyId")
                } else {
                    val reviews = querySnapshot.documents.mapNotNull { document ->
                        val rating = document.get("rating") as? Map<*, *>
                        val name = document.getString("name") ?: "Cliente"
                        val comment = rating?.get("comment") as? String ?: ""
                        val quality = rating?.get("quality") as? Long ?: 0L
                        val punctuality = rating?.get("punctuality") as? Long ?: 0L
                        val service = rating?.get("service") as? Long ?: 0L
                        ReviewItem(name, comment, quality, punctuality, service)
                    }
                    Log.d("fetchReviews", "Avaliações encontradas: ${reviews.size}")
                    reviewsAdapter.submitList(reviews)
                }
            }
            .addOnFailureListener { e ->
                Log.e("fetchReviews", "Erro ao buscar avaliações", e)
            }
    }


    data class ReviewItem(
        val name: String,
        val comment: String,
        val quality: Long,
        val punctuality: Long,
        val service: Long
    )

    companion object {
        fun newInstance(companyId: String): ReviewsFragment {
            return ReviewsFragment().apply {
                arguments = Bundle().apply {
                    putString("companyId", companyId)
                }
            }
        }
    }
}
