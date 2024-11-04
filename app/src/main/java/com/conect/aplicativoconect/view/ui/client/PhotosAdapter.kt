package com.conect.aplicativoconect.view.ui.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.ServicePhoto

class PhotosAdapter(private val photos: List<ServicePhoto>) :
    RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewPhoto)
        private val captionTextView: TextView = itemView.findViewById(R.id.textViewCaption)

        fun bind(photo: ServicePhoto) {
            Glide.with(itemView.context)
                .load(photo.url)
                .placeholder(R.drawable.sem_agendamentos)
                .error(R.drawable.sem_agendamentos)
                .centerCrop()
                .into(imageView)

            captionTextView.text = photo.caption
        }
    }
}
