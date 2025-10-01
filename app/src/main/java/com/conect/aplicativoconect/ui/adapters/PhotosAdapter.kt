package com.conect.aplicativoconect.ui.adapters

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.ServicePhoto

class PhotosAdapter(private val photos: List<ServicePhoto>, private val context: Context) :
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

            // Configurar clique para exibir imagem em tela cheia
            imageView.setOnClickListener {
                showFullScreenImage(photo.url)
            }
        }

        private fun showFullScreenImage(imageUrl: String) {
            val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(R.layout.dialog_fullscreen_image)

            val fullScreenImageView: ImageView = dialog.findViewById(R.id.fullScreenImageView)
            val closeButton: ImageView = dialog.findViewById(R.id.closeButton)

            // Carregue a imagem com Glide
            Glide.with(context)
                .load(imageUrl)
                .into(fullScreenImageView)

            // Configure o clique no botão de voltar para fechar o dialog
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}