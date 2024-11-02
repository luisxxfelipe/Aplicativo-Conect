package com.conect.aplicativoconect.view.ui.admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.ServicePhoto

class ServicePhotosAdapter(
    private val context: Context,
    private val photos: List<ServicePhoto>
) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.item_service_photo, container, false)
        val imageView = view.findViewById<ImageView>(R.id.imageViewPhoto)
        val captionView = view.findViewById<TextView>(R.id.textViewCaption)

        val photo = photos[position]
        Glide.with(context).load(photo.url).into(imageView)
        captionView.text = photo.caption

        container.addView(view)
        return view
    }

    override fun getCount(): Int = photos.size
    override fun isViewFromObject(view: View, obj: Any): Boolean = view == obj
    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
    }
}
