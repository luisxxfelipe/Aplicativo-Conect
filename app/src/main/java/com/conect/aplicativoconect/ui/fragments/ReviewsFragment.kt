package com.conect.aplicativoconect.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.ui.viewmodels.CompanyViewModel
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.ui.adapters.ReviewsAdapter
import com.google.firebase.firestore.FirebaseFirestore

class ReviewsFragment : Fragment() {

    private lateinit var companyId: String
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var reviewsAdapter: ReviewsAdapter
    private val companyViewModel: CompanyViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            companyId = it.getString("companyId") ?: ""
        }
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

        setupObservers()
        if (companyId.isNotEmpty()) {
            companyViewModel.loadReviews(companyId)
        }
    }

    private fun setupObservers() {
        companyViewModel.reviews.observe(viewLifecycleOwner) { reviews ->
            reviewsAdapter.submitList(reviews)
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
