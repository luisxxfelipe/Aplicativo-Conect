package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.conect.aplicativoconect.R
import com.google.android.libraries.places.api.model.Review
import com.google.firebase.firestore.FirebaseFirestore

class ReviewsFragment : Fragment() {

    private lateinit var companyId: String
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            companyId = it.getString("companyId") ?: ""
        }
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reviews, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
    }

    private fun fetchReviews(callback: (List<Review>) -> Unit) {
        firestore.collection("business").document(companyId)
            .collection("reviews")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val reviews = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Review::class.java)
                }
                callback(reviews)
            }
    }

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
