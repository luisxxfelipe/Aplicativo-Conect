package com.conect.aplicativoconect.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.conect.aplicativoconect.R

object ImageHelper {
    // ✅ OTIMIZADO: Carregamento centralizado de imagens de perfil
    fun loadProfileImage(
        context: Context, 
        imageUrl: String?, 
        imageView: ImageView,
        placeholder: Int = R.drawable.foto_perfil_generica,
        errorImage: Int = R.drawable.foto_perfil_generica
    ) {
        Glide.with(context)
            .load(imageUrl)
            .placeholder(placeholder)
            .error(errorImage)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }
    
    // ✅ OTIMIZADO: Carregamento otimizado para business images 
    fun loadBusinessImage(
        context: Context,
        imageUrl: String?,
        imageView: ImageView,
        width: Int = 300,
        height: Int = 200
    ) {
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.foto_perfil_generica)
            .error(R.drawable.foto_perfil_generica)
            .override(width, height)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }
}