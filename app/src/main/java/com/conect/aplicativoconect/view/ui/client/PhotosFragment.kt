package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.ServicePhoto
import com.google.firebase.firestore.FirebaseFirestore

class PhotosFragment : Fragment() {

    private lateinit var companyId: String
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_photos, container, false)
        companyId = arguments?.getString("companyId") ?: ""
        firestore = FirebaseFirestore.getInstance()
        setupViewPager(view)
        return view
    }

    private fun setupViewPager(view: View) {
        val viewPager = view.findViewById<ViewPager2>(R.id.photosViewPager)
        fetchPhotos { photos ->
            viewPager.adapter = PhotosAdapter(photos)
        }
    }

    private fun fetchPhotos(callback: (List<ServicePhoto>) -> Unit) {
        firestore.collection("business").document(companyId)
            .collection("servicePhotos")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val photos = querySnapshot.documents.mapNotNull { document ->
                    val url = document.getString("url")
                    val caption = document.getString("caption")
                    if (url != null) ServicePhoto(url, caption ?: "") else null
                }
                callback(photos)
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
