package com.conect.aplicativoconect.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.ui.viewmodels.CompanyViewModel
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.ServicePhoto
import com.conect.aplicativoconect.ui.adapters.PhotosAdapter
import com.google.firebase.firestore.FirebaseFirestore

class PhotosFragment : Fragment() {

    private lateinit var companyId: String
    private val companyViewModel: CompanyViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photos, container, false)
        companyId = arguments?.getString("companyId") ?: ""
        setupRecyclerView(view)
        return view
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.photosRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 1) // 1 column
        
        companyViewModel.photos.observe(viewLifecycleOwner) { photos ->
            recyclerView.adapter = PhotosAdapter(photos, requireContext())
        }
        
        if (companyId.isNotEmpty()) {
            companyViewModel.loadPhotos(companyId)
        }
    }

    companion object {
        fun newInstance(companyId: String): PhotosFragment {
            return PhotosFragment().apply {
                arguments = Bundle().apply { putString("companyId", companyId) }
            }
        }
    }
}
